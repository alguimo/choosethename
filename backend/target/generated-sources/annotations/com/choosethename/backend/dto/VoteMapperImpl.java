package com.choosethename.backend.dto;

import com.choosethename.backend.model.VoteEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-18T09:33:52+0200",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.15 (Microsoft)"
)
@Component
public class VoteMapperImpl implements VoteMapper {

    @Override
    public VoteEntity toEntity(VoteRequestDTO request, Long listId, Long userId, Long roundId) {
        if ( request == null && listId == null && userId == null && roundId == null ) {
            return null;
        }

        VoteEntity voteEntity = new VoteEntity();

        if ( request != null ) {
            voteEntity.setRankings( mapRankings( request.getRankings() ) );
        }
        voteEntity.setListId( listId );
        voteEntity.setUserId( userId );
        voteEntity.setRoundId( roundId );

        return voteEntity;
    }
}
