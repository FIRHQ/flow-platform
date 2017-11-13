package com.flow.platform.api.test.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flow.platform.api.dao.user.UserDao;
import com.flow.platform.api.dao.user.UserRoleDao;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.domain.user.Role;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.service.user.RoleService;
import com.flow.platform.api.service.user.UserService;
import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * @author liangpengyv
 */
public class UserControllerTest extends ControllerTestWithoutAuth {

    @Autowired
    private UserService userService;

    @Autowired
    private UserDao userDao;

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserRoleDao userRoleDao;

    private User user;

    @Before
    public void beforeTest() {
        user = new User();
        user.setEmail("liangpengyv@fir.im");
        user.setUsername("liangpengyv");
        user.setPassword("liangpengyv");
        userService.register(user, ImmutableList.of(createRole().getName()),
                            false, ImmutableList.of(createFlow().getPath()));
        user.setPassword("liangpengyv");
    }

    @Test
    public void should_login_success_if_request_true() throws Throwable {
        String responseContent;
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder;
        MvcResult mvcResult;

        // login success by email: response 200; return a long token string
        mockHttpServletRequestBuilder = post("/user/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{ \"emailOrUsername\" : \"liangpengyv@fir.im\", \"password\" : \"liangpengyv\" }");

        mvcResult = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isOk()).andReturn();
        responseContent = mvcResult.getResponse().getContentAsString();
        Assert.assertTrue(responseContent.length() > 20);

        // login success by username: response 200; return a long token string
        mockHttpServletRequestBuilder = post("/user/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{ \"emailOrUsername\" : \"liangpengyv\", \"password\" : \"liangpengyv\" }");

        mvcResult = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isOk()).andReturn();
        responseContent = mvcResult.getResponse().getContentAsString();
        Assert.assertTrue(responseContent.length() > 20);

        // login failed: response 400; return error description message
        mockHttpServletRequestBuilder = post("/user/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{ \"emailOrUsername\" : \"xxx\", \"password\" : \"liangpengyv\" }");

        mvcResult = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().is4xxClientError()).andReturn();
        responseContent = mvcResult.getResponse().getContentAsString();
        Assert.assertEquals("{\"message\":\"Illegal email or username\"}", responseContent);

        // login failed: response 500; return error description message
        mockHttpServletRequestBuilder = post("/user/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("abcdefg");

        mvcResult = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().is5xxServerError()).andReturn();
        responseContent = mvcResult.getResponse().getContentAsString();
        Assert.assertEquals("{\"message\"", responseContent.substring(0, 10));
    }

    @Test
    public void should_register_success_if_request_true() throws Throwable {
        String requestContent;
        String responseContent;
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder;
        MvcResult mvcResult;

        // register success: response 200; userinfo is inserted into database
        requestContent = "{ \"email\" : \"testRegister@fir.im\", \"username\" : \"testRegister\", \"password\" : \"liangpengyv\", \"isSendEmail\" : \"false\", "
            + "\"flows\": {\"arrays\": [\"test\"]}, \"roles\": {\"arrays\": [\"admin_test\"]}}";
        mockHttpServletRequestBuilder = post("/user/register").contentType(MediaType.APPLICATION_JSON)
            .content(requestContent);
        mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isOk()).andReturn();
        Assert.assertNotNull(userDao.get("testRegister@fir.im"));

        // register failed: response 400; return error description message
        requestContent = "{ \"email\" : \"testRegister@fir.im\", \"username\" : \"testRegister\", \"password\" : \"liangpengyv\", \"isSendEmail\" : \"false\", "
            + "\"flows\": {\"arrays\": [\"test\"]}, \"roles\": {\"arrays\": [\"admin_test\"]}}";
        mockHttpServletRequestBuilder = post("/user/register").contentType(MediaType.APPLICATION_JSON)
            .content(requestContent);
        mvcResult = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().is4xxClientError()).andReturn();
        responseContent = mvcResult.getResponse().getContentAsString();
        Assert
            .assertEquals("{\"message\":\"Illegal register request parameter: email already exist\"}", responseContent);

        // register failed: response 500; return error description message
        requestContent = "abcdefg";
        mockHttpServletRequestBuilder = post("/user/register").contentType(MediaType.APPLICATION_JSON)
            .content(requestContent);
        mvcResult = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().is5xxServerError()).andReturn();
        responseContent = mvcResult.getResponse().getContentAsString();
        Assert.assertEquals("{\"message\"", responseContent.substring(0, 10));
    }

    @Test
    public void should_delete_user_success() throws Throwable {
        String requestContent;
        String responseContent;
        MvcResult mvcResult;

        Assert.assertNotNull(userDao.get("liangpengyv@fir.im"));

        // delete success: response 200; userinfo is deleted from database
        requestContent = "{ \"arrays\" : [\"liangpengyv@fir.im\"]}";
        mockMvc.perform(post("/user/delete")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestContent)).andExpect(status().isOk()).andReturn();
        Assert.assertNull(userDao.get("liangpengyv@fir.im"));

        //delete failed: response 500; return error description message
        requestContent = "abcdefg";
        mvcResult = mockMvc.perform(post("/user/delete")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestContent)).andExpect(status().is5xxServerError()).andReturn();
        responseContent = mvcResult.getResponse().getContentAsString();
        Assert.assertEquals("{\"message\"", responseContent.substring(0, 10));
    }

    @Test
    public void should_update_users_roles() throws Throwable{
        String requestContent;
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder;

        requestContent = "{ \"emailList\": {\"arrays\": [\"liangpengyv@fir.im\"]}, "
                         + "\"roles\" : {\"arrays\": [\"admin_test\"]}}";
        mockHttpServletRequestBuilder = post("/user/role/update").contentType(MediaType.APPLICATION_JSON)
            .content(requestContent);
        mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isOk()).andReturn();

        Assert.assertEquals(1, userRoleDao.list("liangpengyv@fir.im").size());
    }

    private Role createRole(){
        return roleService.create("admin_test", "");
    }

    private Node createFlow(){
        String path = "test";
        return nodeService.createEmptyFlow(path);
    }

}
