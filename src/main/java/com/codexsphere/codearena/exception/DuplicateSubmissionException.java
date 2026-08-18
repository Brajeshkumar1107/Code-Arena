package com.codexsphere.codearena.exception;

public class DuplicateSubmissionException
        extends RuntimeException {

    public DuplicateSubmissionException(Long submissionId) {

        super("Submission already exists : " + submissionId);

    }

}