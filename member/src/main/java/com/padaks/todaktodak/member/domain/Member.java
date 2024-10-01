package com.padaks.todaktodak.member.domain;

import com.padaks.todaktodak.common.domain.BaseTimeEntity;
import com.padaks.todaktodak.member.dto.DoctorListResDto;
import com.padaks.todaktodak.member.dto.MemberListResDto;
import com.padaks.todaktodak.member.dto.MemberUpdateReqDto;
import lombok.*;
import com.padaks.todaktodak.childparentsrelationship.domain.ChildParentsRelationship;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    private String name;

    @Column(nullable = false, unique = true)
    private String memberEmail;

    //    @Column(nullable = false)
    private String password;

    @Column
    private String profileImgUrl;

    //    @Column(nullable = false, unique = true)
    private String phoneNumber;

    //    @Column(nullable = false)
    private String ssn;

    //    @Column(nullable = false)
    @Column(length = 255)
    private Address address;

    @ColumnDefault("0")
    private int noShowCount;

    @ColumnDefault("0")
    private int reportCount;

    @Column
    @Enumerated(EnumType.STRING)
    private Role role;

    private String bio;

    private Long hospitalId;


    private String fcmToken;

    @Builder.Default
    private boolean verified = false;


    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<ChildParentsRelationship> childParentsRelationshipList;

    @Builder
    public Member(String name, String email, Role role) {
        this.name = name;
        this.memberEmail = email;
        this.role = role;
    }

    public Member update(String name) {
        this.name = name;
        return this;
    }

    public Member updatePass(String uuid) {
        this.password = uuid;
        return this;
    }

    public Member updateAddress(String temp) {
        this.address = new Address(temp, temp, temp);
        return this;
    }
    public Member updateName(String name) {
        this.name = name;
        return this;
    }
    public Member updateFcmToken(String fcmToken){
        this.fcmToken = fcmToken;
        return this;
    }

    // member update 사용
//    public Member updateToEntity(Address address, String name, String phone) {
//        return Member.builder()
//                .address(address != null ? address : this.address) // null 체크
//                .name(name != null ? name : this.name) // null 체크
//                .phoneNumber(phone != null ? phone : this.phoneNumber) // null 체크
//                .build();
//    }

    // 프로필 이미지 URL 변경
    public void changeProfileImgUrl(String newUrl) {
        this.profileImgUrl = newUrl;
    }

    public void updateVerified(){
        this.verified = true;
    }

    // 이름 변경
    public void changeName(String newName) {
        this.name = newName;
    }

    // 전화번호 변경
    public void changePhoneNumber(String newPhoneNumber) {
        this.phoneNumber = newPhoneNumber;
    }

    // 주소 변경
    public void changeAddress(Address newAddress) {
        this.address = newAddress;
    }

    // 비밀번호 변경
    public void changePassword(String newPassword) {
        this.password = newPassword;
    }

    // 의사 약력 변경
    public void changeBio(String newBio){this.bio = newBio;}

    //의사 소속된 병원 변경
    public void changeHospitalId(Long hospitalId){this.hospitalId = hospitalId;}

    public String getRoleKey() {
        return this.role.getKey();
    }

    // 탈퇴 처리 메서드
    public void deleteAccount() {
        this.setDeletedTimeAt(LocalDateTime.now());  // 현재 시간을 삭제 시간으로 설정
    }

    // 유저 목록 조회
    public MemberListResDto listFromEntity(){
        return MemberListResDto.builder()
                .id(this.id)
                .name(this.name)
                .phone(this.phoneNumber)
                .address(this.address)
                .memberEmail(this.memberEmail)
                .role(this.role)
                .build();
    }

    public DoctorListResDto doctorListFromEntity(){
        return DoctorListResDto.builder()
                .id(this.id)
                .name(this.name)
                .profileImgUrl(this.profileImgUrl)
                .build();
    }

}
