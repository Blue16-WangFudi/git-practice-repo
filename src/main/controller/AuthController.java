package cn.blue16.aistudymate.controller;

import cn.blue16.aistudymate.common.api.ApiResponse;
import cn.blue16.aistudymate.dto.auth.LoginRequest;
import cn.blue16.aistudymate.dto.auth.LoginResponse;
import cn.blue16.aistudymate.dto.auth.UserDetailResponse;
import cn.blue16.aistudymate.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * <p>
 * 提供用户认证相关的REST API，包括用户登录、登出、Token刷新和用户信息查询等功能。
 * 使用JWT进行用户身份验证，支持角色基础的访问控制。
 * </p>
 *
 * @author AI Study Mate Team
 * @version 1.0
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 用户登录接口
     * <p>
     * 验证用户提供的用户名和密码，成功后返回JWT令牌和用户基本信息。
     * 支持用户状态检查，包括账户激活状态和锁定状态验证。
     * </p>
     *
     * @param request 登录请求对象，包含用户名和密码
     * @return 包含JWT令牌和用户信息的响应对象
     * @throws BusinessException 当用户名密码错误、账户未激活或被锁定时抛出
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ApiResponse.success("登录成功", response);
    }

    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ApiResponse.success("登录成功", response);
    }

    @PostMapping("/signup")
    public ApiResponse<LoginResponse> signup(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ApiResponse.success("登录成功", response);
    }

    /**
     * 刷新JWT令牌接口
     * <p>
     * 为已认证用户刷新JWT令牌，延长用户会话时间。
     * 从请求中获取当前用户信息并重新生成用户详情响应。
     * </p>
     * <p>
     * 注意：当前实现为简化版本，实际应重新生成新的JWT令牌。
     * </p>
     *
     * @param request HTTP请求对象，包含当前用户的认证信息
     * @return 包含更新后用户信息的响应对象
     * @throws BusinessException 当用户不存在时抛出
     */
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(HttpServletRequest request) {
        String currentUsername = (String) request.getAttribute("currentUsername");
        String currentUserId = (String) request.getAttribute("currentUserId");
        String currentUserRole = (String) request.getAttribute("currentUserRole");

        // 重新生成Token
        UserDetailResponse userDetail = authService.getUserDetail(currentUsername);

        // 构建新的登录响应
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setUserId(userDetail.getUserId());
        userInfo.setUsername(userDetail.getUsername());
        userInfo.setRole(userDetail.getRole());
        userInfo.setStatus(userDetail.getStatus());
        userInfo.setPermissions(userDetail.getPermissions());
        userInfo.setManagedClasses(userDetail.getManagedClasses());

        // 这里简化处理，实际应该重新生成token
        LoginResponse response = new LoginResponse();
        response.setUserInfo(userInfo);

        return ApiResponse.success(response);
    }

    /**
     * 用户登出接口
     * <p>
     * 处理用户登出请求。由于采用JWT无状态认证，服务端不需要维护会话信息，
     * 用户登出主要由客户端删除本地存储的JWT令牌完成。
     * </p>
     *
     * @return 登出成功的响应消息
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        // 由于使用JWT，服务端无状态，登出只需客户端删除token
        return ApiResponse.success("登出成功");
    }

    /**
     * 获取当前用户详细信息接口
     * <p>
     * 根据JWT令牌中的用户信息，返回当前登录用户的详细资料，
     * 包括用户基本信息、角色权限和个人档案等。
     * </p>
     *
     * @param request HTTP请求对象，通过JWT过滤器注入当前用户信息
     * @return 用户详细信息响应对象
     * @throws BusinessException 当用户不存在时抛出
     */
    @GetMapping("/userinfo")
    public ApiResponse<UserDetailResponse> getUserInfo(HttpServletRequest request) {
        String currentUsername = (String) request.getAttribute("currentUsername");
        UserDetailResponse response = authService.getUserDetail(currentUsername);
        return ApiResponse.success(response);
    }
}