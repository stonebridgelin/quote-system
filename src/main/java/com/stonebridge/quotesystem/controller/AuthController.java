package com.stonebridge.quotesystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.stonebridge.quotesystem.common.JwtUtils;
import com.stonebridge.quotesystem.common.Result;
import com.stonebridge.quotesystem.entity.User; // 注意：必须导入你自己的 User 实体类
import com.stonebridge.quotesystem.entity.dto.RegisterDTO;
import com.stonebridge.quotesystem.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public Result<Map<String, String>> login(@RequestBody User loginUser) {
        try {
            // 1. 后端二次校验防越权
            if (!loginUser.getUsername().matches("^[a-zA-Z0-9]+$")) {
                return Result.fail("账号格式非法，仅限字母与数字");
            }

            User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", loginUser.getUsername()));

            // 2. 账号不存在或密码不匹配
            if (user == null || !passwordEncoder.matches(loginUser.getPassword(), user.getPassword())) {
                return Result.fail("账号或密码错误");
            }

            // 3. 生成 Token
            String token = JwtUtils.generateToken(user.getUsername());
            Map<String, String> map = new HashMap<>();
            map.put("token", token);
            map.put("username", user.getUsername());

            // ★ 修复：只传入 data 对象，让你的全局 Result 类自动生成 {code:200, message:"操作成功", data:{...}}
            return Result.success(map);

        } catch (IllegalArgumentException e) {
            // ★ 核心修复：捕获手动向数据库录入“明文密码”导致的 BCrypt 崩溃报错
            return Result.fail("系统检测到数据库密码未加密。请不要手动修改数据库，请在页面重新注册一个新账号登录！");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("服务器内部异常：" + e.getMessage());
        }
    }

    @PostMapping("/register")
    public Result<String> register(@RequestBody RegisterDTO registerUser) {
        try {
            // 后端二次校验
            if (!registerUser.getUsername().matches("^[a-zA-Z0-9]+$") || !registerUser.getPassword().matches("^[a-zA-Z0-9]+$")) {
                return Result.fail("账号和密码只能包含字母和数字");
            }

            Long count = userMapper.selectCount(new QueryWrapper<User>().eq("username", registerUser.getUsername()));
            if (count > 0) {
                return Result.fail("该账号已被注册");
            }

            User newUser = new User();
            newUser.setUsername(registerUser.getUsername());
            // 必须使用 BCrypt 加密后再存入数据库
            newUser.setPassword(passwordEncoder.encode(registerUser.getPassword()));
            newUser.setCreateTime(LocalDateTime.now());
            // ★ 新增：写入用户填写的姓名
            newUser.setName(registerUser.getName());
            // ★ 新增：系统默认赋予角色 (这里以 "USER" 为例，你可以根据业务改为 "普通用户" 或其他枚举)
            newUser.setRole("USER");
            userMapper.insert(newUser);

            // ★ 修复：让 "注册成功，请登录" 成为 data 字段的数据
            return Result.success("注册成功，请登录");

        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("注册失败：" + e.getMessage());
        }
    }
}