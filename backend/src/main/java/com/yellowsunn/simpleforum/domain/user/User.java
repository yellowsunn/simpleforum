package com.yellowsunn.simpleforum.domain.user;

import com.yellowsunn.simpleforum.domain.BaseCreatedTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "username_unique", columnNames = {"username"})
})
@Entity
public class User extends BaseCreatedTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", length = 50)
    private Long id;

    private String username;

    @Column(nullable = false, length = 512)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Role role;

    @Builder
    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.role = Role.USER;
    }

    public void changePassword(String password) {
        this.password = password;
    }

    public void deleteUsername() {
        username = null;
    }
}
