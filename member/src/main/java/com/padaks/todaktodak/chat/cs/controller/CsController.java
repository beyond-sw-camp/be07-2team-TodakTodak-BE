package com.padaks.todaktodak.chat.cs.controller;

import com.padaks.todaktodak.chat.cs.domain.Cs;
import com.padaks.todaktodak.chat.cs.dto.CsCreateReqDto;
import com.padaks.todaktodak.chat.cs.dto.CsResDto;
import com.padaks.todaktodak.chat.cs.dto.CsUpdateReqDto;
import com.padaks.todaktodak.chat.cs.service.CsService;
import com.padaks.todaktodak.common.dto.CommonResDto;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.units.qual.C;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.Path;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/cs")
public class CsController {

    private final CsService csService;

    // 회원 별 CS리스트 조회
    @GetMapping("/list/member/{memberId}")
    public ResponseEntity<?> getCsByMemberId(@PathVariable Long memberId){
        List<CsResDto> csList = csService.getCsByMemberId(memberId);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "회원 별 CS 내역 조회", csList), HttpStatus.OK);
    }

    // CS 생성
    @PostMapping("/create")
    public ResponseEntity<?> createCs(@RequestBody CsCreateReqDto dto){
        Cs cs = csService.createCs(dto);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.CREATED, "CS 생성완료", cs.getId()), HttpStatus.CREATED);
    }

    // CS 수정
    @PostMapping("/update")
    public ResponseEntity<?> updateCs(@RequestBody CsUpdateReqDto dto){
        Cs cs = csService.updateCs(dto);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "CS 내역 수정완료", cs.getId()), HttpStatus.OK);
    }

    // CS id로 CS 조회
    @GetMapping("/detail/cs-id/{id}")
    public ResponseEntity<?> detailCsByCsId(@PathVariable Long id){
        CsResDto csResDto = csService.detailCsByCsId(id);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "CS내역 조회 성공 : CS id로 조회", csResDto), HttpStatus.OK);
    }

    // 채팅방 id로 CS 조회
    @GetMapping("/detail/chatroom-id/{chatRoomId}")
    public ResponseEntity<?> detailCsByChatRoomId(@PathVariable Long chatRoomId){
        List<CsResDto> csResDtoList = csService.detailCsByChatRoomId(chatRoomId);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "CS내역 조회 성공 : 채팅방 id로 조회", csResDtoList), HttpStatus.OK);
    }

    // CS 삭제
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteCs(@PathVariable Long id){
        csService.deleteCs(id);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "CS 삭제 성공", null), HttpStatus.OK);
    }

}
