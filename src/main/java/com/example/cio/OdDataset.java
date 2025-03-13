package com.example.cio;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import javax.json.JsonObjectBuilder;
import javax.json.JsonArrayBuilder;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonArray;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import java.io.OutputStream;
import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.ByteOrder;

import javax.sql.DataSource;
import java.sql.*;

import java.util.concurrent.*;
import java.util.List;

@Path("oddataset")
public class OdDataset {
    private class ResponseOutput implements StreamingOutput {
        private String title;
        public ResponseOutput(String title) {
            if(title == null || title.isEmpty()) {
                throw new IllegalArgumentException("The title argument is either null or an empty string.");
            }
            this.title = title;
        }

        class ResultSetReader implements Callable<Boolean> {
            ResultSet rs;
            Charset utf8 = Charset.forName("UTF-8");

            public ResultSetReader(ResultSet rs) {
                this.rs = rs;
            }

            public byte[] resultBytes = null;

            @Override
            public Boolean call() throws Exception {
                resultBytes = row2json().toString().getBytes(utf8);
                return rs.next();
            }

            private void addString(JsonArrayBuilder ab, String val) {
                if(val == null) {
                    ab.addNull();
                } else {
                    ab.add(val);
                }
            }

            private JsonObject row2json() throws SQLException {
                JsonObjectBuilder ob = Json.createObjectBuilder();
                JsonArrayBuilder ab = Json.createArrayBuilder();
                ab.add(rs.getLong("dataset_id"));
                addString(ab, rs.getString("dataset_title"));
                addString(ab, rs.getString("dataset_name"));
                addString(ab, rs.getString("publisher"));
                addString(ab, rs.getString("creator"));
                addString(ab, rs.getString("group_title"));
                addString(ab, rs.getString("frequency_of_update"));
                addString(ab, rs.getString("description"));
                addString(ab, rs.getString("release_day"));
                ob.add("r", ab);
                return ob.build();
            }
        }

        class ResultSetWriter implements Callable<Boolean> {
            OutputStream os;

            public ResultSetWriter(OutputStream os) {
                this.os = os;
            }

            public byte[] textToWrite = null;

            private byte[] lenb = new byte[Integer.BYTES];

            @Override
            public Boolean call() throws Exception {
                if(textToWrite != null) {
                    ByteBuffer.wrap(lenb).order(ByteOrder.BIG_ENDIAN).putInt(textToWrite.length);
                    os.write(lenb);
                    os.write(textToWrite);
                }
                return true;
            }
        }

        @Override
        public void write(OutputStream os) throws IOException, WebApplicationException {
            ExecutorService es = Executors.newFixedThreadPool(2);
            try {
                InitialContext context = new InitialContext();
                DataSource dataSource = (DataSource)context.lookup("java:/comp/env/jdbc/cio");
                Connection conn = dataSource.getConnection();
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT * FROM oddataset WHERE dataset_title like '%" + title + "%'");
                try (conn; st; rs) {
                    boolean nextAvailable = rs.next();
                    ResultSetReader reader = new ResultSetReader(rs);
                    ResultSetWriter writer = new ResultSetWriter(os);
                    List<Callable<Boolean>> tasks = List.of(reader, writer);
                    while(nextAvailable) {
                        List<Future<Boolean>> fs = es.invokeAll(tasks);
                        writer.textToWrite = reader.resultBytes;
                        reader.resultBytes = null;
                        try {
                            nextAvailable = fs.get(0).get() && fs.get(1).get();
                        }
                        catch(ExecutionException e) {
                            throw e.getCause();
                        }
                    }
                }
            }
            catch(InterruptedException e) {
                throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
            }
            catch(SQLException e) {
                throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
            }
            catch(NamingException e) {
                throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
            }
            catch(Throwable e) {
                throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
            }
            finally {
                if(es != null) {
                    es.shutdown();
                }
            }
        }
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response get(@QueryParam("title") String title) {
        StreamingOutput stream = new ResponseOutput(title);
        return Response.ok(stream).build();
    }
}
