package com.codexsphere.codearena.service;

import com.codexsphere.codearena.entity.TestCase;

import java.util.List;

public interface TestCaseService {

    List<TestCase> getTestCases(Long problemId);

}
