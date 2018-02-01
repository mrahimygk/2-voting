package dao;

import db.DatabaseConnection;
import entity.Candidate;
import entity.People;
import entity.Voting;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class VotingDAO {


    private static VotingDAO ins = null;
    private DatabaseConnection connection;

    public synchronized static VotingDAO getInstance(DatabaseConnection connection) {
        if (ins == null) {
            ins = new VotingDAO(connection);
        }
        return ins;
    }

    private VotingDAO(DatabaseConnection connection) {
        this.connection = connection;
    }


    /**
     * insert a vote into database
     * if already voted -> just change count and change update time
     * if already voted to the other option -> change it
     *
     * @param voting
     * @return
     */
    public boolean insert(Voting voting) {
        // TODO: check for previous votes
        People people = voting.getVoter();
        Candidate candidate = voting.getCandidate();

        // Already voted for this ... no need to change?
        if (PeopleDAO.getInstance(connection).hasVoted(voting, people, candidate.getId(), true)) {
            try {
                PreparedStatement statement = connection.getDatabaseConnection()
                        .prepareStatement(
                                "UPDATE voting SET " +
                                        "last_change = ? " +
                                        "changes_count=changes_count+1 " +
                                        "WHERE id = ?;"
                        );

                String date = LocalDate.now().toString();
                int i = 1;
                statement.setString(i++, date);
                statement.setInt(i++, Integer.parseInt(voting.getId()));

                statement.executeUpdate();
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return false;
        }

        if (PeopleDAO.getInstance(connection).hasVotedToOpponent(voting, people, candidate, true)) {
            // TODO: change vote and set opponent

            voting.setCandidate(candidate.getOpponent());

            // persist changes
            try {
                PreparedStatement statement = connection.getDatabaseConnection()
                        .prepareStatement(
                                "UPDATE voting SET " +
                                        "candidate_id = ? , " +
                                        "last_change = ? , " +
                                        "changes_count=changes_count+1 " +
                                        "WHERE id = ?;"
                        );

                String date = LocalDate.now().toString();
                int i = 1;
                statement.setInt(i++, Integer.parseInt(voting.getCandidate().getId()));
                statement.setString(i++, date);
                statement.setInt(i++, Integer.parseInt(voting.getId()));
                statement.executeUpdate();
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return false;
        }


        try {
            PreparedStatement statement = connection.getDatabaseConnection()
                    .prepareStatement(
                            "INSERT INTO voting (" +
                                    "voter_id ," +
                                    "candidate_id, " +
                                    "first_voted," +
                                    "last_change," +
                                    "changes_count" +
                                    ") " +
                                    "VALUES (?,?,?,?,1); "
                    );

            String date = LocalDate.now().toString();
            int i = 1;
            statement.setString(i++, people.getId());
            statement.setInt(i++, Integer.parseInt(candidate.getId()));
            statement.setString(i++, date);
            statement.setString(i++, date);

            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * remove a voting from database
     * checks for keys?
     *
     * @param voting
     * @return
     */
    public boolean delete(Voting voting) {
        try {
            PreparedStatement statement = connection.getDatabaseConnection()
                    .prepareStatement(
                            "DELETE FROM voting WHERE id=?; "
                    );

            statement.setInt(1, Integer.parseInt(voting.getId()));
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * updates a voting
     *
     * @param voting
     * @return
     */
    public boolean update(Voting voting) {
        try {
            PreparedStatement statement = connection.getDatabaseConnection()
                    .prepareStatement(
                            "UPDATE voting SET " +
                                    "voter_id=? ," +
                                    "candidate_id=? ," +
                                    "first_voted=? ," +
                                    "last_change=? ," +
                                    "changes_count=? " +
                                    "WHERE id = ?;"
                    );

            int i = 1;
            statement.setString(i++, voting.getVoter().getId());
            statement.setInt(i++, Integer.parseInt(voting.getCandidate().getId()));
            statement.setString(i++, voting.getFirstVoted());
            statement.setString(i++, voting.getLastChange());
            statement.setInt(i++, voting.getChangesCount());
            statement.setInt(i++, Integer.parseInt(voting.getId()));

            statement.executeUpdate();

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * get all the votes by everyone
     *
     * @return
     */
    public List<Voting> getAll() {
        List<Voting> votingList = new ArrayList<>();

        try {
            PreparedStatement statement = connection.getDatabaseConnection()
                    .prepareStatement(
                            "SELECT * FROM voting; "
                    );


            ResultSet result = statement.executeQuery();
            while (result.next()) {
                int i = 1;
                String id = String.valueOf(result.getInt(i++));
                Voting voting = new Voting(
                        id,
                        PeopleDAO.getInstance(connection).get(false, result.getString(i++)),
                        CandidateDAO.getInstance(connection).get(result.getString(i++)),
                        result.getString(i++),
                        result.getString(i++),
                        result.getInt(i++)
                );

                votingList.add(voting);

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return votingList;
    }


    public List<Voting> getAllForPeople(String peopleID) {
        List<Voting> votingList = new ArrayList<>();

        try {
            PreparedStatement statement = connection.getDatabaseConnection()
                    .prepareStatement(
                            "SELECT * FROM voting WHERE voter_id=?; "
                    );

            statement.setString(1, peopleID);
            ResultSet result = statement.executeQuery();
            while (result.next()) {
                int i = 1;
                String id = String.valueOf(result.getInt(i++));
                Voting voting = new Voting(
                        id,
                        PeopleDAO.getInstance(connection).get(false, result.getString(i++)),
                        CandidateDAO.getInstance(connection).get(result.getString(i++)),
                        result.getString(i++),
                        result.getString(i++),
                        result.getInt(i++)
                );

                votingList.add(voting);

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return votingList;
    }

    public boolean get(Voting voting) {
        try {
            PreparedStatement statement = connection.getDatabaseConnection()
                    .prepareStatement(
                            "SELECT * FROM voting WHERE voter_id=? AND candidate_id=?; "
                    );

            int k = 1;
            statement.setString(k++, voting.getVoter().getId());
            statement.setInt(k++, Integer.parseInt(voting.getCandidate().getId()));

            ResultSet result = statement.executeQuery();
            if (result.next()) {
                int i = 1;
                voting.setId(String.valueOf(result.getInt(i++)));
                i++; // people
                i++; // candidate

                voting.setFirstVoted(result.getString(i++)); // first voted
                i++; // lastChange
                voting.setChangesCount(result.getInt(i++));
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public Voting get(String id) {
        Voting voting = null;
        try {
            PreparedStatement statement = connection.getDatabaseConnection()
                    .prepareStatement(
                            "SELECT * FROM voting WHERE id=?; "
                    );

            statement.setInt(1, Integer.parseInt(id));
            ResultSet result = statement.executeQuery();
            while (result.next()) {
                int i = 1;
                voting = new Voting(
                        id,
                        PeopleDAO.getInstance(connection).get(false, result.getString(i++)),
                        CandidateDAO.getInstance(connection).get(result.getString(i++)),
                        result.getString(i++),
                        result.getString(i++),
                        result.getInt(i++)
                );

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return voting;
    }

}
