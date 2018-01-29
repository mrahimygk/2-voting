package worker;

import db.DatabaseConnection;
import db.DatabaseUtils;
import entity.People;
import entity.Voting;
import entity.Canditate;
import redis.clients.jedis.Jedis;
import org.json.JSONObject;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

class Worker {
    public static void main(String[] args) {
        System.out.println("Starting Java Worker - Modified By Local");

        //try {
        Jedis redis = DatabaseConnection.getInstance().connectToRedis("redis");
        System.err.println("Watching vote queue");

        System.err.println("DEBUG ::: AVOIDING TABLE INITIALIZATION FOR THE BEHALF OF " +
                "/docker-entrypoint-initdb.d/init.sql");

        //Connection dbConn = DatabaseConnection.getInstance().connectToDB();
        /*if (!DatabaseConnection.getInstance().isTablesInitialized()){
            DatabaseConnection.getInstance().connect();
            DatabaseConnection.getInstance().close();
            System.err.println("DEBUG ::: TABLES INITIALIZED");
        }*/
        boolean exit = false;
        while (!exit) {
            // this is a blocking pop.
            String voteJSON = redis.blpop(0, "votes").get(1);
            System.err.printf("We have json file : %s \n", voteJSON);
            JSONObject voteData = new JSONObject(voteJSON);
            String voterId = voteData.getString("voter_id");
            String lastVisit = voteData.getString("last_visit");
            String userAddress = voteData.getString("user_address");
            String userAgent = voteData.getString("user_agent");
            String vote = voteData.getString("vote");
            String port = voteData.getString("port");

            System.err.printf("Processing vote for '%s' by '%s'\n", vote, voterId);

            Candidate candidate = DatabaseUtils.getInstance().getCandidate(vote);

            People people = new People(
                    voterId, "", userAgent, userAddress,
                    port, "", lastVisit, null);

            DatabaseUtils.getInstance().getPeople(people);

            Voting voting = new Voting("", people, candidate, "",
                    new Date().toString(), 0);

            List<Voting> votingList = new ArrayList<>();
            votingList.add(voting);
            people.setVoteList(votingList);

            if (DatabaseUtils.getInstance().insertPeople(people)) {
                System.err.printf("Insert people (id= %s) is done ", people.getId());
                // TODO: INSERT vote
            }
        }
    }

    /**
     * We update vote in another way:
     * PeopleDAO INSERTS every vote in insert() method
     */
    static void updateVote(Connection dbConn, String voterID, String vote) throws SQLException {
        // todo: update
        PreparedStatement insert = dbConn.prepareStatement(
                "INSERT INTO votes (id, vote) VALUES (?, ?)");
        insert.setString(1, voterID);
        insert.setString(2, vote);

        try {
            insert.executeUpdate();
        } catch (SQLException e) {
            PreparedStatement update = dbConn.prepareStatement(
                    "UPDATE votes SET vote = ? WHERE id = ?");
            update.setString(1, vote);
            update.setString(2, voterID);
            update.executeUpdate();
        }
    }

}
