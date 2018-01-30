package dao;

import db.DatabaseConnection;
import entity.Candidate;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CandidateDAO {


    private static CandidateDAO ins = null;
    private DatabaseConnection connection;

    public synchronized static CandidateDAO getInstance(DatabaseConnection connection) {
        if (ins == null) {
            ins = new CandidateDAO(connection);
        }
        return ins;
    }

    public CandidateDAO(DatabaseConnection connection) {
        this.connection = connection;
    }

    /**
     * insert a single candidate. This is very bad. need to change previous insertion's
     * opponent id by hand
     *
     * @param candidate
     * @return
     */
    public boolean insert(Candidate candidate) {
        try {
            PreparedStatement statement = connection.getDatabaseConnection()
                    .prepareStatement(
                            "INSERT INTO candidate (name, opponent_id) VALUES (?,?); "
                    );

            int i = 1;
            statement.setString(i++, candidate.getName());
            statement.setInt(i++, Integer.parseInt(candidate.getOpponent().getId()));

            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean delete(Candidate candidate) {
        try {
            PreparedStatement statement = connection.getDatabaseConnection()
                    .prepareStatement(
                            "DELETE FROM candidate WHERE id=?; "
                    );

            int i = 1;
            statement.setInt(i++, Integer.parseInt(candidate.getId()));

            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean update(Candidate candidate) {
        throw new RuntimeException("Not Implemented");
    }

    public List<Candidate> getAll() {
        List<Candidate> candidates = new ArrayList<>();
        try {
            PreparedStatement statement = connection.getDatabaseConnection()
                    .prepareStatement(
                            "SELECT * FROM candidate; "
                    );


            ResultSet result = statement.executeQuery();
            while (result.next()) {
                int i = 1;

                Candidate candidate = new Candidate(
                        String.valueOf(result.getInt(i++)),
                        result.getString(i++),
                        null
                );

                // opponent ID
                candidate.setOpponent(getOpponent(candidate, result.getString(i++)));
                candidates.add(candidate);
            }
        } catch (
                SQLException e)

        {
            e.printStackTrace();
        }

        return candidates;
    }

    public Candidate getOpponent(Candidate candidate, String opID) throws SQLException {
        Candidate opponent = null;
        PreparedStatement statement = connection.getDatabaseConnection()
                .prepareStatement(
                        "SELECT * FROM candidate WHERE id=?; "
                );

        statement.setInt(1, Integer.parseInt(opID));
        ResultSet resultSet = statement.executeQuery();
        if (resultSet.next()) {
            int j = 1;
            opponent = new Candidate(
                    String.valueOf(resultSet.getInt(j++)),
                    resultSet.getString(j++),
                    candidate
            );
        }

        return opponent;
    }

    public Candidate get(String id) {
        Candidate candidate = null;
        try {
            PreparedStatement statement = connection.getDatabaseConnection()
                    .prepareStatement(
                            "SELECT * FROM candidate WHERE id = ?; "
                    );

            statement.setInt(1, Integer.parseInt(id));
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                int i = 1;

                candidate = new Candidate(
                        result.getString(i++),
                        result.getString(i++),
                        null
                );

                // opponent ID
                candidate.setOpponent(getOpponent(candidate, result.getString(i++)));
            }
        } catch (
                SQLException e)

        {
            e.printStackTrace();
        }

        return candidate;
    }

    public JSONArray getPollAsJsonArray(){
        JSONArray candidates = new JSONArray();
        try {

            CallableStatement statement = connection.getDatabaseConnection()
                    .prepareCall("{call fetch_candidates();}");

//            opt = [{'id':row[0], 'name':row[1]}, { 'id': row[2] , 'name':row[3] }]

            ResultSet result = statement.executeQuery();
            while (result.next()) {
                int i = 1;
                JSONArray array = new JSONArray();

                JSONObject object1 = new JSONObject();
                object1.put("id", result.getInt(i++) );
                object1.put("name", result.getString(i++) );
                JSONObject object2 = new JSONObject();
                object2.put("id", result.getInt(i++) );
                object2.put("name", result.getString(i++) );

                array.put(object1);
                array.put(object2);
                candidates.put(array);
            }
        } catch (
                SQLException e)

        {
            e.printStackTrace();
        }

        return candidates;
    }
}
