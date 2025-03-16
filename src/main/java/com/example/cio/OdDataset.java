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
import java.io.UncheckedIOException;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.ByteOrder;

import javax.sql.DataSource;
import java.sql.*;

import java.util.concurrent.*;
import java.util.List;
import java.util.ArrayList;

@Path("oddataset")
public class OdDataset {
    private class ResponseOutput implements StreamingOutput,Runnable {
        private String title;
        public ResponseOutput(String title) {
            if(title == null || title.isEmpty()) {
                throw new IllegalArgumentException("The title argument is either null or an empty string.");
            }
            this.title = title;
        }

        Charset utf8 = Charset.forName("UTF-8");

        private void addString(JsonArrayBuilder ab, String val) {
            if(val == null) {
                ab.addNull();
            } else {
                ab.add(val);
            }
        }

        private JsonObject row2json(ResultSet rs) throws SQLException {
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

        private Exception lastError = null;

        @Override
        public void run() {
            byte[] lenb = new byte[Integer.BYTES];
            lastError = null;
            try {
                record2write.forEach(e -> {
                    byte[] txt2write = e.toString().getBytes(utf8);
                    ByteBuffer.wrap(lenb).order(ByteOrder.BIG_ENDIAN).putInt(txt2write.length);
                    try {
                        os.write(lenb);
                        os.write(txt2write);
                    }
                    catch(IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
                });
            }
            catch(Exception e) {
                lastError = e;
            }
        }

        ArrayList<JsonObject> record2write = new ArrayList();

        private ArrayList<JsonObject> loadNext(ResultSet rs, int n) throws SQLException {
            ArrayList<JsonObject> records = new ArrayList(n);
            for(int i = 0; i < n && rs.next(); i++) {
                records.add(row2json(rs));
            }
            return records;
        }

        OutputStream os;

        @Override
        public void write(OutputStream os) throws IOException, WebApplicationException {
            this.os = os;
            ExecutorService es = Executors.newSingleThreadExecutor();
            try {
                InitialContext context = new InitialContext();
                DataSource dataSource = (DataSource)context.lookup("java:/comp/env/jdbc/cio");
                Connection conn = dataSource.getConnection();
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT * FROM oddataset WHERE dataset_title like '%" + title + "%'");
                try (conn; st; rs) {
                    ArrayList<JsonObject> nextwrite = loadNext(rs, 100);
                    while(nextwrite.size() > 0) {
                        record2write = nextwrite;
                        Future f = es.submit(this);
                        nextwrite = loadNext(rs, 100);
                        try {
                            f.get();
                        }
                        catch(ExecutionException e) {
                            throw e.getCause();
                        }
                        if(lastError != null) {
                            throw lastError;
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
                this.os = null;
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
