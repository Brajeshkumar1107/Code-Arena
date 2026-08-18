package com.codexsphere.codearena.service.impl;

import com.codexsphere.codearena.entity.TestCase;
import com.codexsphere.codearena.repository.TestCaseRepository;
import com.codexsphere.codearena.service.TestCaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TestCaseServiceImpl
        implements TestCaseService {

    private final TestCaseRepository repository;

    @Override
    public List<TestCase> getTestCases(Long problemId) {

        return repository.findByProblemIdOrderByOrderNo(problemId);

    }

}
