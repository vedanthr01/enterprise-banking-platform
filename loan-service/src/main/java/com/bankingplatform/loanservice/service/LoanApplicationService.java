package com.bankingplatform.loanservice.service;

import com.bankingplatform.loanservice.dto.EmiScheduleResponse;
import com.bankingplatform.loanservice.dto.LoanApplicationRequest;
import com.bankingplatform.loanservice.dto.LoanApplicationResponse;
import com.bankingplatform.loanservice.dto.LoanResponse;

import java.util.List;

public interface LoanApplicationService {
    LoanApplicationResponse apply(String applicantUserId, LoanApplicationRequest request);
    LoanApplicationResponse getApplication(String applicationId, String requesterUserId, boolean isAdmin);
    List<LoanApplicationResponse> getMyApplications(String applicantUserId);
    LoanResponse getLoan(String loanId, String requesterUserId, boolean isAdmin);
    List<EmiScheduleResponse> getSchedule(String loanId, String requesterUserId, boolean isAdmin);
}
