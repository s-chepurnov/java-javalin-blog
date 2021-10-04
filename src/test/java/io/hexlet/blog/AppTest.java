package io.hexlet.blog;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import static org.assertj.core.api.Assertions.assertThat;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import io.javalin.Javalin;
import io.ebean.DB;
import io.ebean.Transaction;

import io.hexlet.blog.domain.Article;
import io.hexlet.blog.domain.query.QArticle;

class AppTest {

    @Test
    void testInit() {
        assertThat(true).isEqualTo(true);
    }

    private static Javalin app;
    private static String baseUrl;
    private static Article existingArticle;
    private static Transaction transaction;

    @BeforeAll
    public static void beforeAll() {
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;

        existingArticle = new Article("example name", "example description");
        existingArticle.save();
    }

    @AfterAll
    public static void afterAll() {
        app.stop();
    }

    // В данном случае тесты не влияют друг на друга,
    // но при использовании БД запускать каждый тест в транзакции -
    // это хорошая практика
    @BeforeEach
    void beforeEach() {
        transaction = DB.beginTransaction();
    }

    @AfterEach
    void afterEach() {
        transaction.rollback();
    }

    @Nested
    class RootTest {

        @Test
        void testIndex() {
            HttpResponse<String> response = Unirest.get(baseUrl).asString();
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getBody()).contains("Привет от Хекслета!");
        }
    }

    @Nested
    class UrlTest {

        @Test
        void testIndex() {
            HttpResponse<String> response = Unirest
                .get(baseUrl + "/articles")
                .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(body).contains(existingArticle.getName());
        }

        @Test
        void testShow() {
            HttpResponse<String> response = Unirest
                .get(baseUrl + "/articles/" + existingArticle.getId())
                .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(body).contains(existingArticle.getName());
            assertThat(body).contains(existingArticle.getDescription());
        }

        @Test
        void testStore() {
            String inputName = "new name";
            String inputDescription = "new description";
            HttpResponse<String> responsePost = Unirest
                .post(baseUrl + "/articles")
                .field("name", inputName)
                .field("description", inputDescription)
                .asEmpty();

            assertThat(responsePost.getStatus()).isEqualTo(302);
            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/articles");

            HttpResponse<String> response = Unirest
                .get(baseUrl + "/articles")
                .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(body).contains(inputName);
            assertThat(body).contains("Статья успешно создана");

            Article actualArticle = new QArticle()
                .name.equalTo(inputName)
                .findOne();

            assertThat(actualArticle).isNotNull();
            assertThat(actualArticle.getName()).isEqualTo(inputName);
            assertThat(actualArticle.getDescription()).isEqualTo(inputDescription);
        }
    }
}
