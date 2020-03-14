package com.cxb.sso.passport.service;

import com.cxb.sso.passport.model.User;

public interface UserService {

    Boolean checkUser(String username, String password);

    User getByUsernameAndPassword(String username, String password);
}
