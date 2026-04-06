package com.openclassrooms.etudiant.mapper;

import com.openclassrooms.etudiant.dto.UserDTO;
import com.openclassrooms.etudiant.dto.UserProfileDTO;
import com.openclassrooms.etudiant.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper for User entity and UserDTO conversion
 * Maps between DTO and entity with proper field name handling
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserDtoMapper {

    @Mapping(target = "password", ignore = true) // never expose password in DTO
    UserDTO toDto(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "refreshToken", ignore = true) // not part of UserDTO
    User toEntity(UserDTO userDTO);

    // password and audit fields are excluded by MapStruct — safe for login response
    UserProfileDTO toProfileDto(User user);
}
