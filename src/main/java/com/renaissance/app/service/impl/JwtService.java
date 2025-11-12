package com.renaissance.app.service.impl;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.renaissance.app.exception.ResourcesNotFoundException;
import com.renaissance.app.model.User;
import com.renaissance.app.repository.IUserRepository;

@Service
public class JwtService {

    private final IUserRepository userRepository;

    public JwtService(IUserRepository userRepository) {
    	this.userRepository=userRepository;
    }

    public Long getUserIdFromAuthentication() throws ResourcesNotFoundException {
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
 		if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
 			throw new ResourcesNotFoundException("User not authenticated");
 		}
 		User user=  userRepository.findByUsername(auth.getName())
 				.orElseThrow(() -> new ResourcesNotFoundException("Authenticated user not found"));
 		
 		return user.getUserId();
 	}
}
