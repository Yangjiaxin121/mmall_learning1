package com.mmall.service.impl;

import com.mmall.common.Const;
import com.mmall.common.ServerResponse;
import com.mmall.dao.UserMapper;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import com.mmall.util.MD5Util;
import com.mmall.util.RedisShardedPoolUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("iUserService")
public class UserServiceImpl implements IUserService {


    @Autowired
    private UserMapper userMapper;

    /**
     * 登录
     * @param username
     * @param password
     * @return
     */
    @Override
    public ServerResponse<User> login(String username, String password) {
        System.out.println("service:"+username);
        int resultCount = userMapper.checkUsername(username);
        if (resultCount == 0){
            return ServerResponse.createByErrorMessage("用户名不存在");
        }

        // 密码登录MD5
        String md5Password = MD5Util.MD5EncodeUtf8(password);

        User user = userMapper.selectLogin(username,md5Password);
        if (user == null){
            return ServerResponse.createByErrorMessage("密码错误");
        }

        user.setPassword(StringUtils.EMPTY);
        return ServerResponse.createBySuccess("登录成功",user);
    }

    /**
     * 注册
     * @param user
     * @return
     */
    public ServerResponse<String> register(User user){

        ServerResponse validResponse = checkValid(user.getUsername(),Const.USERNAME);
        if (!validResponse.isSuccess()){
            return validResponse;
        }

        validResponse = checkValid(user.getEmail(),Const.EMAIL);
        if (!validResponse.isSuccess()){
            return validResponse;
        }

        //MD5加密
        user.setPassword(MD5Util.MD5EncodeUtf8(user.getPassword()));

        int resultCount = userMapper.insert(user);
        if (resultCount == 0){
            return ServerResponse.createByErrorMessage("注册失败");
        }

        return ServerResponse.createBySuccessMessage("注册成功");

    }

    /**
     * 查看是否存在该用户
     * @param str
     * @param type
     * @return
     */
    public ServerResponse<String> checkValid(String str, String type){
        if (StringUtils.isNotBlank(type)){
            //开始校验
            if (Const.USERNAME.equals(type)){
                int resultcount = userMapper.checkUsername(str);
                if (resultcount > 0){
                    return ServerResponse.createByErrorMessage("用户名已存在");
                }
            }
            if (Const.EMAIL.equals(type)){
                int resultcount = userMapper.checkEmail(str);
                if (resultcount > 0){
                    return ServerResponse.createByErrorMessage("Email已存在");
                }
            }


        }else{
            return ServerResponse.createByErrorMessage("参数错误");
        }
        return ServerResponse.createBySuccessMessage("校验成功");
    }


    /**
     * 查看找回密码的问题
     * @param username
     * @return
     */
    public ServerResponse<String> selectQuestion(String username){

        ServerResponse validResponse = checkValid(username,Const.USERNAME);
        if (validResponse.isSuccess()){
            //用户不存在
            return ServerResponse.createBySuccessMessage("用户不存在");
        }
        String question = userMapper.selectQuestionByUsername(username);
        if (StringUtils.isNotBlank(question)){
            return ServerResponse.createBySuccess(question);
        }

        return ServerResponse.createByErrorMessage("找回密码的问题是空的");
    }

    /**
     * 检查问题和答案是否正确，并将forgetToken存放在本地，时间为12小时  在修改密码的时候用到，否则为横向越权
     * 使用本地缓存检查问题答案
     * @param username
     * @param question
     * @param answer
     * @return
     */
    public ServerResponse<String> checkAnswer(String username, String question, String answer){
        int resultcount = userMapper.checkAnswer(username,question,answer);
        if (resultcount > 0){
            //说明问题及问题的答案是这个用户的，并且是正确的
            String forgetToken = UUID.randomUUID().toString();
            //把forgetToken放在本地，设置它的有效期
            //TokenCache.setKey(TokenCache.TOKEN_PREFIX+username,forgetToken);
            RedisShardedPoolUtil.setEx(Const.TOKEN_PREFIX+username,forgetToken,60*60*12);
            return ServerResponse.createBySuccess(forgetToken);
        }
        return ServerResponse.createBySuccessMessage("问题的答案错误");
    }

    /**
     * 忘记密码里的重置密码
     * @param username
     * @param passwordNew
     * @param forgetToken
     * @return
     */
    public ServerResponse<String> forgetRestPassword(String username, String passwordNew, String forgetToken){
        if (StringUtils.isBlank(forgetToken)){
            return ServerResponse.createByErrorMessage("参数错误，token需要传递");
        }


        ServerResponse validResponse = checkValid(username,Const.USERNAME);
        if (validResponse.isSuccess()){
            //用户不存在
            return ServerResponse.createBySuccessMessage("用户不存在");
        }
        //String token = TokenCache.getKey(TokenCache.TOKEN_PREFIX+username);
        String token = RedisShardedPoolUtil.get(Const.TOKEN_PREFIX+username);

        if (StringUtils.isBlank(token)){
            return ServerResponse.createByErrorMessage("token无效或者过期");
        }
        if (StringUtils.equals(token,forgetToken)){
            String md5Password = MD5Util.MD5EncodeUtf8(passwordNew);
            int rowCount = userMapper.updatePasswordByUsername(username,md5Password);
            if (rowCount > 0){
                return ServerResponse.createBySuccessMessage("修改密码成功");
            }
        }else{
            return ServerResponse.createByErrorMessage("token错误，请重新获取充值密码的token");
        }
        return ServerResponse.createByErrorMessage("修改密码失败");

    }


    /**
     * 重置密码（不是忘记密码里的）
     * @param passwordOld
     * @param passwordNew
     * @param user
     * @return
     */

    public ServerResponse<String> resetPassword(String passwordOld, String passwordNew, User user){
        //防止横向越权，需要校验一下这个用户的旧密码，一定要指定是这个用户，因为我们会查询一个count(1)，如果不指定id结果就是true
        int resultCount = userMapper.checkPassword(MD5Util.MD5EncodeUtf8(passwordOld),user.getId());
        if (resultCount == 0){
            return ServerResponse.createByErrorMessage("旧密码错误");
        }
        user.setPassword(MD5Util.MD5EncodeUtf8(passwordNew));
        int updateCount = userMapper.updateByPrimaryKeySelective(user);
        if (updateCount > 0){
            return ServerResponse.createBySuccessMessage("密码修改成功");
        }

        return ServerResponse.createByErrorMessage("密码更新失败");

    }


    /**
     * 更新用户信息
     * @param user
     * @return
     */

    public ServerResponse<User> updateInformation(User user){

        //uusername是不能被更新的
        //email也要进行校验，校验新的email是不是已经存在，并且不能是当前用户的
        int resultCount = userMapper.checkEmailByUserId(user.getEmail(),user.getId());
        if (resultCount > 0){
            return ServerResponse.createByErrorMessage("email已存在，请更换email尝试再更新");
        }
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setEmail(user.getEmail());
        updateUser.setPhone(user.getPhone());
        updateUser.setQuestion(user.getQuestion());
        updateUser.setAnswer(user.getAnswer());

        int updateCount = userMapper.updateByPrimaryKeySelective(updateUser);
        if (updateCount > 0){
            return ServerResponse.createBySuccess("更新个人信息成功",updateUser);
        }

        return ServerResponse.createByErrorMessage("更新个人信息失败");
    }


    /**
     * 获取用户信息
     * @param userId
     * @return
     */
    public ServerResponse<User> getInformation(Integer userId){
        User user = userMapper.selectByPrimaryKey(userId);
        if (user == null){
            return ServerResponse.createByErrorMessage("找不到当前用户");
        }
        user.setPassword(StringUtils.EMPTY);
        return ServerResponse.createBySuccess(user);
    }


    /**
     * 校验是否是管理员
     * @param user
     * @return
     */
    public ServerResponse checkAdminRole(User user){
        if (user != null && user.getRole().intValue() == Const.Role.ROLE_ADMIN){
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByError();
    }





}
