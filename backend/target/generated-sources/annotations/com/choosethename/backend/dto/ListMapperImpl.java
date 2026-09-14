package com.choosethename.backend.dto;

import com.choosethename.backend.model.ListEntity;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-14T19:05:10+0200",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.15 (Microsoft)"
)
@Component
public class ListMapperImpl implements ListMapper {

    @Override
    public ListResponseDTO toResponseDTO(ListEntity entity, List<String> members, String ownerUsername) {
        if ( entity == null && members == null && ownerUsername == null ) {
            return null;
        }

        ListResponseDTO listResponseDTO = new ListResponseDTO();

        if ( entity != null ) {
            listResponseDTO.setId( entity.getId() );
            listResponseDTO.setName( entity.getName() );
            listResponseDTO.setInvitationCode( entity.getInvitationCode() );
            listResponseDTO.setCodeExpiresAt( entity.getCodeExpiresAt() );
            listResponseDTO.setPhase( entity.getPhase() );
            listResponseDTO.setInvitationsOpen( entity.isInvitationsOpen() );
        }
        List<String> list = members;
        if ( list != null ) {
            listResponseDTO.setMembers( new ArrayList<String>( list ) );
        }
        listResponseDTO.setOwnerUsername( ownerUsername );

        return listResponseDTO;
    }
}
