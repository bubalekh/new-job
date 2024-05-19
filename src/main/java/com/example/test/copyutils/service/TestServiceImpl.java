package com.example.test.copyutils.service;

import com.example.test.copyutils.CopyUtils;
import com.example.test.copyutils.model.Man;

public class TestServiceImpl implements TestService {
    @Override
    public Man copyMan(Man reference) {
        return (Man) CopyUtils.deepCopy(reference);
    }
}
