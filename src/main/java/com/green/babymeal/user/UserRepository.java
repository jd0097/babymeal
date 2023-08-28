package com.green.babymeal.user;

import com.green.babymeal.common.config.security.model.ProviderType;
import com.green.babymeal.common.entity.UserEntity;
import com.green.babymeal.user.model.UserVo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    UserEntity findByProviderTypeAndUid(ProviderType providerType, String uid);
    UserEntity findByUid(String uid);
    //UserBabyinfoEntity findByGenderTypeAndGenger(GenderType genderType, Character gender);

}
