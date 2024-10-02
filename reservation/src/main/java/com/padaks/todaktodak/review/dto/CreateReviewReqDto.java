package com.padaks.todaktodak.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class CreateReviewReqDto {

//    해당 리뷰 id
    private Long id;
    private int rating;
    private String contents;

}
