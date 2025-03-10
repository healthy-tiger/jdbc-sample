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
import javax.json.JsonWriter;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import java.io.OutputStream;
import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.ByteOrder;

import java.sql.*;

@Path("oddataset")
public class OdDataset {
    static JsonArray columns;
    static {
        columns = Json.createArrayBuilder()
        .add("dataset_id")
        .add("dataset_title")
        .add("dataset_name")
        .add("publisher")
        .add("creator")
        .add("group_title")
        .add("frequency_of_update")
        .add("description")
        .add("release_day")
        .build();
    }

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

    private class ResponseOutput implements StreamingOutput {
        private String title;
        public ResponseOutput(String title) {
            this.title = title;
        }

        @Override
        public void write(OutputStream os) throws IOException, WebApplicationException {
            Connection conn = null;
            try {
                InitialContext context = new InitialContext();
                DataSource dataSource = (DataSource)context.lookup("java:/comp/env/jdbc/cio");
                conn = dataSource.getConnection();

                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(title == "" ? "SELECT * FROM oddataset" : "SELECT * FROM oddataset WHERE dataset_title like '%" + title + "%'");

                Charset utf8 = Charset.forName("UTF-8");
                try {
                    byte[] lenb = new byte[4];
                    while(rs.next()) {
                        byte[] text = row2json(rs).toString().getBytes(utf8);
                        ByteBuffer.wrap(lenb).order(ByteOrder.BIG_ENDIAN).putInt(text.length);
                        os.write(lenb);
                        os.write(text);
                    }
                }
                catch(SQLException e) {
                    throw new IOException(e);
                }
                finally {
                    try {
                        rs.close();
                        st.close();
                    }
                    catch(Exception e) {
                        throw new IOException(e);
                    }
                }

            }
            catch(SQLException e) {
                throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
            }
            catch(javax.naming.NamingException e) {
                throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
            }
            catch(NumberFormatException e) {
                throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
            }
            finally {
                try {
                    if(conn != null) {
                        conn.close();
                    }
                } catch(SQLException e) {
                    e.printStackTrace();
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
