package net.ma7boub.tpjwtspring.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record RegisterRequest
        (
                String email, @JsonAlias("password") String pass
        ) {

}
