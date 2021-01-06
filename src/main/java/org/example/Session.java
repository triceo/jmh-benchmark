package org.example;

import org.optaplanner.core.api.score.buildin.simple.SimpleScore;

interface Session {

    int insert(Object object);

    int update(Object object);

    SimpleScore calculateScore();

    void close();
}
