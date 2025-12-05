package com.smartparking.accounts_service.repo;
import com.smartparking.accounts_service.model.LoginCode;

import java.util.Optional;
public interface LoginCodeRepository {
    LoginCode save(LoginCode code);
    Optional<LoginCode> findValidByCode(String code);
    public Optional<LoginCode> findValidByAccountId(String accountid);
    LoginCode markUsed(LoginCode code);

}
