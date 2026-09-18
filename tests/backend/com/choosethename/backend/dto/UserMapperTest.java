package com.choosethename.backend.dto;

import com.choosethename.backend.model.Role;
import com.choosethename.backend.model.User;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    @Test
    void shouldMapUserToUserDTO() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testUser");
        user.setPasswordHash("hashed_password");
        user.setRole(Role.PARTICIPANT);

        UserDTO dto = mapper.toDTO(user);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getUsername()).isEqualTo("testUser");
        assertThat(dto.getRole()).isEqualTo(Role.PARTICIPANT);
    }
}
