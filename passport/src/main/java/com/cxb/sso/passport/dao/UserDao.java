package com.cxb.sso.passport.dao;

import com.cxb.sso.passport.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDao extends JpaRepository<User, Integer> {

    User getByUsername(String username);

    User getByUsernameAndPassword(String username, String password);
}
