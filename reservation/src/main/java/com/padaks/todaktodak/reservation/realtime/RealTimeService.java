package com.padaks.todaktodak.reservation.realtime;

import com.google.firebase.database.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class RealTimeService {

    private final FirebaseDatabase database= FirebaseDatabase.getInstance();
    private final DatabaseReference databaseReference= database.getReference("data");

    public void addWaitingLine(WaitingTurnDto turnDto) {
        update(turnDto.reservationId, turnDto.getTurnNumber());
    }

    public void reset(String Id, String newData) {
        log.info("create");
        // Firebase Database 인스턴스를 가져옴
        // "medi"라는 경로에 대한 참조 생성

        // Medi 객체 생성
        Map<String, Object> create = new HashMap<>();
        create.put("id", Id);
        create.put("data", newData);

        // Firebase Realtime Database에 데이터 저장 (CompletionListener 사용)
        databaseReference.setValue(create, (error, ref) -> {
            if (error != null) {
                // 데이터 저장에 실패한 경우
                log.info("Failed to save data: " + error.getMessage());
            } else {
                // 데이터가 성공적으로 저장된 경우
                log.info("Data saved successfully");
            }
        });
    }
    //  데이터 중 특정 필드만 업데이트하는 메서드
    public void update(String reservationId, String newData) {
        DatabaseReference userRef = databaseReference.child(reservationId);

        // 업데이트할 데이터 설정
        Map<String, Object> updates = new HashMap<>();
        updates.put("id", reservationId);
        updates.put("data", newData);


        // Firebase에서 데이터 업데이트
        userRef.updateChildren(updates, (error, ref) -> {
            if (error != null) {
                System.out.println("Failed to update data: " + error.getMessage());
            } else {
                System.out.println("User data updated successfully");
            }
        });
    }

    public void delete(String reservationId){
        DatabaseReference userRef = databaseReference.child(reservationId);

        userRef.removeValue((error, ref) -> {
            if(error != null){
                System.out.println("Failed to delete data: " + error.getMessage());
            }else{
                System.out.println("User data deleted successfully");
            }
        });
    }

    // 데이터를 한 번 읽어오는 메서드
    public void readDataOnce(String id) {
        databaseReference.orderByChild("id").equalTo(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Message message = snapshot.getValue(Message.class);
                        if (message != null) {
                            System.out.println("Data found: " + message);
                        }
                    }
                } else {
                    System.out.println("No data found with id: " + id);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("Error fetching data: " + databaseError.getMessage());
            }
        });
    }
}