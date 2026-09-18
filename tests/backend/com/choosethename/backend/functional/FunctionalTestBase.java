package com.choosethename.backend.functional;

import com.choosethename.backend.repository.ListMembershipRepository;
import com.choosethename.backend.repository.ListRepository;
import com.choosethename.backend.repository.NameRepository;
import com.choosethename.backend.repository.SharedNamePoolRepository;
import com.choosethename.backend.repository.UserRepository;
import com.choosethename.backend.repository.VoteRepository;
import com.choosethename.backend.repository.VotingRoundRepository;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class FunctionalTestBase {

    protected static final String PASSWORD = "Password123";

    @LocalServerPort
    protected int port;

    protected SystemClient client;

    @Autowired protected UserRepository userRepository;
    @Autowired protected ListRepository listRepository;
    @Autowired protected ListMembershipRepository membershipRepository;
    @Autowired protected NameRepository nameRepository;
    @Autowired protected SharedNamePoolRepository sharedNamePoolRepository;
    @Autowired protected VoteRepository voteRepository;
    @Autowired protected VotingRoundRepository votingRoundRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        client = new RestAssuredSystemClient(port);
        cleanDatabase();
    }

    protected String registerAndLogin(String username) {
        client.register(username, PASSWORD);
        Response login = client.login(username, PASSWORD);
        return login.jsonPath().getString("accessToken");
    }

    protected void cleanDatabase() {
        voteRepository.deleteAll();
        votingRoundRepository.deleteAll();
        sharedNamePoolRepository.deleteAll();
        nameRepository.deleteAll();
        membershipRepository.deleteAll();
        listRepository.deleteAll();
        userRepository.deleteAll();
    }
}