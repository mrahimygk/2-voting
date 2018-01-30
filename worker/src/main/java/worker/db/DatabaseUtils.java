package db;

import dao.PeopleDAO;
import dao.CandidateDAO;
import dao.VotingDAO;
import entity.People;
import entity.Candidate;
import entity.Voting;

import java.sql.SQLException;

public class DatabaseUtils {

    private static DatabaseConnection connection = DatabaseConnection.getInstance();
    private static DatabaseUtils instance = null;

    public static DatabaseUtils getInstance() {
        if (instance == null)
            instance = new DatabaseUtils();

        return instance;
    }

    public boolean insertPeople(People people) {
        System.err.printf("MY_DEBUG (%s.java) : inserting people with id= %s\n", getClass().getSimpleName(), people.getId());
        boolean success = false;
        if (connection.connect()) {
            success = PeopleDAO.getInstance(connection).insert(people);
            connection.close();
        }

        System.err.printf("MY_DEBUG (%s.java) : (DONE) inserting people with id= %s\n", getClass().getSimpleName(), people.getId());
        return success;

    }

    public boolean insertCandidate(Candidate candidate) {
        return CandidateDAO.getInstance(connection).insert(candidate);
    }

    public boolean insertVoting(Voting voting) {

        return VotingDAO.getInstance(connection).insert(voting);

    }

    public Candidate getCandidate(String candidateID) {

        Candidate candidate = null;
        if (connection.connect()) {
            candidate = CandidateDAO.getInstance(connection).get(candidateID);
            connection.close();
        }

        return candidate;
    }

    public Candidate getCandidateOpponent(Candidate candidate) {
        Candidate rCandidate = null;
        if (connection.connect()) {
            try {
                rCandidate = CandidateDAO.getInstance(connection).getOpponent(candidate, candidate.getOpponent().getId());
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                connection.close();
            }
        }

        return rCandidate;
    }

    public boolean fillPeople(People people) {
        boolean isFilled = false;
        if (connection.connect()) {
            isFilled = PeopleDAO.getInstance(connection).get(people);
            connection.close();
        }

        return isFilled;
    }

}
