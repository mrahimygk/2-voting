package worker;

import db.DatabaseConnection;
import db.DatabaseUtils;
import entity.People;
import entity.Voting;
import entity.Candidate;
import redis.clients.jedis.Jedis;
import org.json.JSONObject;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;

class Worker {
    public static void main(String[] args) {
        System.out.println("Starting Java Worker - Modified By Local");

        //try {
        Jedis redis = DatabaseConnection.getInstance().connectToRedis("redis");
        System.err.println("Watching vote queue");

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
                    port, "", lastVisit, new ArrayList<Voting>());

            if(!DatabaseUtils.getInstance().fillPeople(people)){
                // not filled:
                people.setFullName("New Comer");
                people.setFirstVisit(people.getLastVisit());
            }

            Voting voting = new Voting("", people, candidate, "",
                    new Date().toString(), 0);
            if(!DatabaseUtils.getInstance().fillVoting(voting)){
                //todo: fill voting from this data
                // id comes from SERIAL
                voting.setFirstVoted(voting.getLastChange());
            }

            people.getVoteList().add(voting);

            if (DatabaseUtils.getInstance().insertPeople(people)) {
                System.err.printf("Insert people (id= %s) is done ", people.getId()); // یا درج میشه یا آپدیت
                // TODO: INSERT vote
                if (DatabaseUtils.getInstance().insertVoting(voting)){
                    System.err.printf("Insert Voting (id= %s) is done ", voting.getId());
                }
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
