package kata.academy.eurekalikeservice.outer;

import kata.academy.eurekalikeservice.SpringSimpleContextTest;
import kata.academy.eurekalikeservice.api.Data;
import kata.academy.eurekalikeservice.feign.ContentServiceFeignClient;
import kata.academy.eurekalikeservice.model.entity.PostLike;
import kata.academy.eurekalikeservice.util.ApiValidationUtil;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.constraints.Positive;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MockBeans({
        @MockBean(ContentServiceFeignClient.class)
})
public class PostLikeRestControllerIT extends SpringSimpleContextTest {

    @Autowired
    private ContentServiceFeignClient contentServiceFeignClient;

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, value = "/scripts/outer/PostLikeRestController/addPostLike_SuccessfulTest/After.sql")
    public void addPostLike_SuccessfulTest() throws Exception {
        Long postId = 1L;
        Long userId = 1L;
        doReturn(Boolean.TRUE).when(contentServiceFeignClient).existsByPostId(postId);
        boolean positive = true;
        mockMvc.perform(post("/api/v1/likes/posts/{postId}", postId)
                        .header("userId", userId.toString())
                        .param("positive", String.valueOf(positive))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
//                .andExpect(MockMvcResultMatchers.jsonPath("$.id", Is.is(1)))
//                .andExpect(MockMvcResultMatchers.jsonPath("$.postId", Is.is(postId.intValue())))
//                .andExpect(MockMvcResultMatchers.jsonPath("$.userId", Is.is(userId.intValue())))
//                .andExpect(MockMvcResultMatchers.jsonPath("$.positive", Is.is(positive)));
        assertTrue(entityManager.createQuery(
                        """
                                SELECT COUNT(pl.id) > 0
                                FROM PostLike pl
                                WHERE pl.postId = :postId
                                AND pl.userId = :userId
                                AND pl.positive = :positive
                                """, Boolean.class)
                .setParameter("postId", postId)
                .setParameter("userId", userId)
                .setParameter("positive", positive)
                .getSingleResult());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, value = "/scripts/outer/PostLikeRestController/addPostLike_PostLikeExistsTest/Before.sql")
    @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, value = "/scripts/outer/PostLikeRestController/addPostLike_PostLikeExistsTest/After.sql")
    public void addPostLike_PostLikeExistsTest() throws Exception {
        Long postId = 1L;
        Long userId = 1L;
        doReturn(Boolean.TRUE).when(contentServiceFeignClient).existsByPostId(postId);
        mockMvc.perform(post("/api/v1/likes/posts/{postId}", postId)
                        .header("userId", userId.toString())
                        .param("positive", String.valueOf(true))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.text", Is.is(
                        String.format("Пользователь userId %d уже лайкнул пост postId %d",
                                postId, userId)
                )));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, value = "/scripts/outer/PostLikeRestController/addPostLike_PostLikeExistsTest/Before.sql")
    @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, value = "/scripts/outer/PostLikeRestController/addPostLike_PostLikeExistsTest/After.sql")
    public void updatePostLike_SuccessfulTest() throws Exception {
        Long postId = 1L;
        long userId = 1L;
        doReturn(Boolean.TRUE).when(contentServiceFeignClient).existsByPostId(postId);
        mockMvc.perform(put("/api/v1/likes/posts/{postId}", postId)
                        .header("userId", Long.toString(userId))
                        .param("positive", String.valueOf(true))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void deletePostLike_PostLikeUnsuccessTest() throws Exception {
        long postId = 31L;
        long userId = 1L;
        doReturn(Boolean.TRUE).when(contentServiceFeignClient).existsByPostId(postId);
        mockMvc.perform(delete("/api/v1/likes/posts/{postId}", postId)
                        .header("userId", Long.toString(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.text", Is.is(
                        String.format("Лайк с postId %d, userId %d нет в базе данных", postId, userId)
                )));
    }

    @Test
    public void getPostLikeCount_SuccessfulTest() throws Exception {
        long postId = 12L;
        String positiveState = String.valueOf(true);
        doReturn(Boolean.TRUE).when(contentServiceFeignClient).existsByPostId(postId);
        mockMvc.perform(get("/api/v1/likes/posts/{postId}/count", postId)
                        .param("positive", positiveState)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data", Is.is(4)));
    }

}
