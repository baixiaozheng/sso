package com.cxb.sso.passport.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "user")
public class User implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4166320705984354442L;

	@Id
	@Column(name = "id", nullable = false)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Getter @Setter
	private Integer id;

	@Column(name = "username", unique = true)
	@Getter @Setter
	private String username;

	@Column(name = "password")
	@Getter @Setter
	private String password;


}
 