package com.yellowsunn.simpleforum.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yellowsunn.simpleforum.api.SessionConst;
import com.yellowsunn.simpleforum.api.dto.user.UserGetDto;
import com.yellowsunn.simpleforum.api.dto.user.UserLoginDto;
import com.yellowsunn.simpleforum.api.dto.user.UserPatchRequestDto;
import com.yellowsunn.simpleforum.api.dto.user.UserRegisterDto;
import com.yellowsunn.simpleforum.api.service.UserService;
import com.yellowsunn.simpleforum.api.util.RefererFilter;
import com.yellowsunn.simpleforum.domain.user.Role;
import com.yellowsunn.simpleforum.domain.user.User;
import com.yellowsunn.simpleforum.domain.user.repository.UserRepository;
import com.yellowsunn.simpleforum.exception.ForbiddenException;
import com.yellowsunn.simpleforum.exception.NotFoundException;
import com.yellowsunn.simpleforum.exception.PasswordMismatchException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserApiController.class)
class UserApiControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    UserService userService;

    @MockBean
    UserRepository userRepository;

    @MockBean
    User mockUser;

    @MockBean
    RefererFilter refererFilter;

    ObjectMapper objectMapper = new ObjectMapper();

    Long userId = 1L;
    Role userRole = Role.USER;

    @Test
    @DisplayName("???????????? ??????")
    void register() throws Exception {
        //given
        UserRegisterDto dto = getTestUserRegisterDto();
        MockHttpServletRequestBuilder request = registerRequest();
        setJsonContent(request, dto);
        //then
        mvc.perform(request).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("???????????? ?????? ??????")
    void validationFailForRegistration() throws Exception {
        //given
        List<UserRegisterDto> dtos = new ArrayList<>();
        dtos.add(UserRegisterDto.builder().username(" ").password("12345678").build());

        dtos.add(UserRegisterDto.builder().username("username").password("").build());
        dtos.add(UserRegisterDto.builder().username("username").password("1234567").build());
        dtos.add(UserRegisterDto.builder().username("username").password("12345678901234567").build());

        MockHttpServletRequestBuilder request = registerRequest();
        //then
        for (UserRegisterDto dto : dtos) {
            setJsonContent(request, dto);
            mvc.perform(request)
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    @DisplayName("?????? ?????? ????????? ?????? ??????")
    void duplicateRegistration() throws Exception {
        //given
        UserRegisterDto dto = getTestUserRegisterDto();

        MockHttpServletRequestBuilder request = registerRequest();
        setJsonContent(request, dto);

        //mocking
        doThrow(DataIntegrityViolationException.class).when(userService).register(dto);

        //then
        mvc.perform(request)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("????????? ??????(?????? ??????)")
    void login() throws Exception {
        //given
        UserLoginDto dto = getTestUserLoginDto();
        MockHttpServletRequestBuilder request = loginRequest();
        setJsonContent(request, dto);

        //mocking
        given(mockUser.getId()).willReturn(userId);
        given(mockUser.getRole()).willReturn(userRole);
        given(userService.login(dto)).willReturn(mockUser);

        //then
        ResultActions resultActions = mvc.perform(request)
                .andExpect(status().isOk());

        expectExistLoginSession(resultActions);
    }

    @Test
    @DisplayName("????????? ?????? ??????(?????? ??????x)")
    void validationFailForLogin() throws Exception {
        //given
        List<UserLoginDto> dtos = new ArrayList<>();
        dtos.add(UserLoginDto.builder().username(" ").password("12345678").build());

        dtos.add(UserLoginDto.builder().username("username").password("").build());
        dtos.add(UserLoginDto.builder().username("username").password("1234567").build());
        dtos.add(UserLoginDto.builder().username("username").password("12345678901234567").build());

        MockHttpServletRequestBuilder request = loginRequest();

        for (UserLoginDto dto : dtos) {
            setJsonContent(request, dto);
            //then
            ResultActions resultActions = mvc.perform(request)
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    @DisplayName("????????? ?????? ??????(?????? ??????x)")
    void inValidUserLogin() throws Exception {
        //given
        UserLoginDto dto = getTestUserLoginDto();
        MockHttpServletRequestBuilder request = loginRequest();
        setJsonContent(request, dto);

        //mocking
        doThrow(NotFoundException.class).when(userService).login(dto);

        //then
        ResultActions resultActions = mvc.perform(request)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("?????? ???????????? ?????? ??????(????????? ????????? ???????????? ???)")
    void findCurrentLoggedInUser() throws Exception {
        //given
        UserGetDto userGetDto = getTestUserGetDto();

        MockHttpServletRequestBuilder request = getCurrentUserRequest();
        setLoginSession(request);

        //mocking
        given(userService.findUserById(userGetDto.getId())).willReturn(userGetDto);

        mvc.perform(request)
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("?????? ???????????? ?????? ?????? ?????? ??? ?????? ?????????(????????? ????????? ?????? ?????? ???)")
    void FailedToFindCurrentLoggedInUser() throws Exception {
        //given
        MockHttpServletRequestBuilder request = getCurrentUserRequest();
        setLoginSession(request);

        //mocking
        given(userService.findUserById(userId)).willThrow(NotFoundException.class);

        //then
        ResultActions resultActions = mvc.perform(request)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("?????? ???????????? ?????? ?????? ?????? ??????")
    void unauthorizedForFindCurrentUser() throws Exception {
        //given
        MockHttpServletRequestBuilder request = getCurrentUserRequest();

        //then
        ResultActions resultActions = mvc.perform(request);
        expectUnauthorized(resultActions);
    }

    @Test
    @DisplayName("???????????? ?????? ??????")
    void changePassword() throws Exception {
        //given
        UserPatchRequestDto dto = getTestUserPatchRequestDto();
        MockHttpServletRequestBuilder request = patchCurrentUserRequest();
        setLoginSession(request);
        setJsonContent(request, dto);

        //then
        mvc.perform(request)
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("???????????? ?????? ?????? ??????")
    void unauthorizedForChangePassword() throws Exception {
        //given
        MockHttpServletRequestBuilder request = patchCurrentUserRequest();

        //then
        ResultActions resultActions = mvc.perform(request);
        expectUnauthorized(resultActions);
    }

    @Test
    @DisplayName("???????????? ?????? ?????? ??????")
    void validationFailedForChangePassword() throws Exception {
        //given
        List<UserPatchRequestDto> dtos = new ArrayList<>();
        dtos.add(UserPatchRequestDto.builder().password("").newPassword("password2").build());
        dtos.add(UserPatchRequestDto.builder().password("password").newPassword("").build());
        dtos.add(UserPatchRequestDto.builder().password("password").newPassword("passwor").build());
        dtos.add(UserPatchRequestDto.builder().password("password").newPassword("passwordpasswordp").build());

        MockHttpServletRequestBuilder request = patchCurrentUserRequest();
        setLoginSession(request);

        for (UserPatchRequestDto dto : dtos) {
            setJsonContent(request, dto);

            mvc.perform(request)
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    @DisplayName("???????????? ??????????????? ?????? ?????? ??????")
    void failedToFindUserWhoWantChangePassword() throws Exception {
        //given
        UserPatchRequestDto dto = getTestUserPatchRequestDto();
        MockHttpServletRequestBuilder request = patchCurrentUserRequest();
        setLoginSession(request);
        setJsonContent(request, dto);

        //mocking
        doThrow(NotFoundException.class).when(userService).changePassword(userId, dto);

        //then
        ResultActions resultActions = mvc.perform(request)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("???????????? ?????? ????????? ?????? ???????????? ????????? ???????????? ?????? ?????? ??????")
    void invalidOldPassword() throws Exception {
        //given
        UserPatchRequestDto dto = getTestUserPatchRequestDto();
        MockHttpServletRequestBuilder request = patchCurrentUserRequest();
        setLoginSession(request);
        setJsonContent(request, dto);

        //mocking
        doThrow(PasswordMismatchException.class).when(userService).changePassword(userId, dto);

        //then
        mvc.perform(request)
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("?????? ???????????? ?????? ?????? ??????")
    void deleteCurrentUser() throws Exception {
        //given
        MockHttpServletRequestBuilder request = deleteCurrentUserRequest();
        setLoginSession(request);

        //then
        mvc.perform(request)
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("?????? ?????? ?????? ??????")
    void unauthorizedForDeleteUser() throws Exception {
        //given
        MockHttpServletRequestBuilder request = deleteCurrentUserRequest();

        //then
        ResultActions resultActions = mvc.perform(request);
        expectUnauthorized(resultActions);
    }

    @Test
    @DisplayName("??????????????? ?????? ?????? ??????")
    void failedToFindUserWhoWantDelete() throws Exception {
        //given
        MockHttpServletRequestBuilder request = deleteCurrentUserRequest();
        setLoginSession(request);

        //mocking
        doThrow(NotFoundException.class).when(userService).deleteCurrentUser(userId);
        
        //then
        mvc.perform(request)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("???????????? ?????? ?????? ??????")
    void deleteById() throws Exception {
        //given
        MockHttpServletRequestBuilder request = deleteByIdRequest(userId);
        setLoginSession(request);
        request.sessionAttr(SessionConst.USER_ROLE, Role.ADMIN);

        //then
        mvc.perform(request)
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("???????????? ?????? ?????? ?????? ??????")
    void unauthorizedForDeleteById() throws Exception {
        //given
        MockHttpServletRequestBuilder request = deleteByIdRequest(userId);

        //then
        mvc.perform(request)
                .andExpect(status().isUnauthorized());
    }
    @Test
    @DisplayName("???????????? ?????? ?????? ?????? - ?????? ??????")
    void forbiddenDeleteById() throws Exception {
        //given
        MockHttpServletRequestBuilder request = deleteByIdRequest(userId);
        setLoginSession(request);

        //then
        mvc.perform(request)
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("???????????? ?????? ?????? ?????? - ???????????? ????????? ??? ??????")
    void failedToDeleteById() throws Exception {
        //given
        MockHttpServletRequestBuilder request = deleteByIdRequest(userId);
        setLoginSession(request);
        request.sessionAttr(SessionConst.USER_ROLE, Role.ADMIN);

        //mocking
        doThrow(ForbiddenException.class).when(userService).deleteById(userId);

        //then
        mvc.perform(request)
                .andExpect(status().isForbidden());
    }

    private MockHttpServletRequestBuilder registerRequest() {
        return post("/api/users");
    }

    private MockHttpServletRequestBuilder loginRequest() {
        return post("/api/users/login");
    }

    private MockHttpServletRequestBuilder getCurrentUserRequest() {
        return get("/api/users/current");
    }

    private MockHttpServletRequestBuilder patchCurrentUserRequest() {
        return patch("/api/users/current");
    }

    private MockHttpServletRequestBuilder deleteCurrentUserRequest() {
        return delete("/api/users/current");
    }

    private MockHttpServletRequestBuilder deleteByIdRequest(Long id) {
        return delete("/api/users/" + id);
    }

    private void setLoginSession(MockHttpServletRequestBuilder builder) {
        getSession().forEach((key, value) -> builder.sessionAttr(key, value));
    }

    private void setJsonContent(MockHttpServletRequestBuilder builder, Object object) throws JsonProcessingException {
        builder
                .content(objectMapper.writeValueAsString(object))
                .contentType(MediaType.APPLICATION_JSON);
    }

    private void expectUnauthorized(ResultActions resultActions) throws Exception {
        resultActions
                .andExpect(status().isUnauthorized());
    }

    private void expectExistLoginSession(ResultActions resultActions) throws Exception {
        for (String key : getSession().keySet()) {
            resultActions.andExpect(request().sessionAttribute(key, getSession().get(key)));
        }
    }

    private Map<String, Object> getSession() {
        Map<String, Object> session = new HashMap<>();
        session.put(SessionConst.USER_ID, userId);
        session.put(SessionConst.USER_ROLE, userRole);

        return session;
    }

    private UserRegisterDto getTestUserRegisterDto() {
        return UserRegisterDto.builder()
                .username("username")
                .password("12345678")
                .build();
    }

    private UserLoginDto getTestUserLoginDto() {
        return UserLoginDto.builder()
                .username("username")
                .password("12345678")
                .build();
    }

    private UserGetDto getTestUserGetDto() {
        UserGetDto userGetDto = new UserGetDto();
        userGetDto.setId(userId);
        userGetDto.setUsername("username");
        userGetDto.setRole(Role.USER);

        return userGetDto;
    }

    private UserPatchRequestDto getTestUserPatchRequestDto() {
        return UserPatchRequestDto.builder()
                .password("password").newPassword("password2").build();
    }
}