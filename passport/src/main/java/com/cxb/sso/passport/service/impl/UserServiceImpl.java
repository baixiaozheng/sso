package com.cxb.sso.passport.service.impl;

import com.cxb.sso.passport.dao.UserDao;
import com.cxb.sso.passport.model.User;
import com.cxb.sso.passport.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDao userDao;

    @Override
    public Boolean checkUser(String username, String password) {
        Boolean result = false;
        User user = userDao.getByUsername(username);
        if (user != null) {
            if(password.equals(user.getPassword())){
                result = true;
            }
        }
        return result;
    }

    @Override
    public User getByUsernameAndPassword(String username, String password) {
        return userDao.getByUsernameAndPassword(username, password);
    }
}
