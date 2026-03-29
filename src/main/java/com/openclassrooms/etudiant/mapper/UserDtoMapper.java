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

    @Mapping(target = "password", ignore = true) // Security: don't expose password in DTO
    UserDTO toDto(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "refreshToken", ignore = true) // Optional field - not in UserDTO yet
    User toEntity(UserDTO userDTO);

    // Convert User to UserProfileDTO for safe login response
    // Only maps: id, login, firstName, lastName (password and audit fields excluded
    // by design)
    UserProfileDTO toProfileDto(User user);
}
