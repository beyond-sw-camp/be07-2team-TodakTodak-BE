package com.padaks.todaktodak.member.service;



import com.padaks.todaktodak.member.domain.Member;
import com.padaks.todaktodak.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;


@RequiredArgsConstructor
@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    @Autowired
    private final MemberRepository memberRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        // OAuth2UserSevice를 통해 가져온 OAuth2User의 attribute를 저장
        OAuthAttributes attributes = OAuthAttributes.of(registrationId,
                userNameAttributeName, oAuth2User.getAttributes());

//        System.out.println("서비스입니다!!!!!!!!!!! 왓나요!!!!!!!!!!1");
//        System.out.println(attributes);

        Member member = null;
        member = saveOrUpdate(attributes);



        if (member.getName() == null){
            String temp = "이름을 입력해주세요";
            member.updateName(temp);
        }
        if (member.getPassword() == null) {
            String uuidPass = String.valueOf(UUID.randomUUID());
            member.updatePass(uuidPass);
        }
        if (member.getAddress() == null){
            String address = "임시주소입니다. 주소를 변경해주세요";
            member.updateAddress(address);
        }
        if (member.getPhoneNumber() == null){
            String phone = "핸드폰 번호를 입력해주세요";
            member.updatePhoneNumber(phone);
        }
//        if (member.isVerified() == false) {
//            member.updateVerified();
//        }
        memberRepository.save(member);
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(member.getRoleKey())),
                attributes.getAttributes(), attributes.getNameAttributeKey());
    }

    private Member saveOrUpdate(OAuthAttributes attributes) {

        Member member = memberRepository.findByMemberEmail(attributes.getMemberEmail())
                .map(entity -> entity.update(attributes.getName()))
                .orElse(attributes.toEntity());
        return memberRepository.save(member);
    }
}