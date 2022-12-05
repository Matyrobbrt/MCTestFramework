package com.matyrobbrt.testframework.impl;

import com.matyrobbrt.testframework.Test;
import org.jetbrains.annotations.ApiStatus;

import java.util.Iterator;

@ApiStatus.Internal
public record SummaryDumper(TestFrameworkInternal framework) {
    public String createLoggingSummary() {
        final StringBuilder summary = new StringBuilder();

        final Iterator<Test> itr = framework.tests().all().iterator();
        while (itr.hasNext()) {
            final Test test = itr.next();
            final Test.Status status = framework.tests().getStatus(test.id());
            summary.append("\tTest ").append(test.id()).append(":\n");
            summary.append("\t\t").append(status.result()).append(status.message().isBlank() ? "" : " - " + status.message());
            if (itr.hasNext()) summary.append('\n');
        }

        return summary.toString();
    }
}
