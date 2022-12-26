package com.matyrobbrt.testframework.impl;

import com.matyrobbrt.testframework.group.Group;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

record DisabledTestData(Set<String> enabledTestIds, Set<String> enabledGroups) {
    static DisabledTestData get() {
        final ProcessHandle ph = ProcessHandle.current();
        return ph.info().arguments().map(args -> {
            final DisabledTestData testData = new DisabledTestData(new HashSet<>(), new HashSet<>());
            for (int i = 0; i < args.length; i++) {
                final String arg = args[i];
                if (arg.equals("--enableTest")) {
                    testData.enabledTestIds.add(args[++i]);
                } else if (arg.equals("--enableTestGroup")) {
                    testData.enabledGroups.add(args[++i]);
                }
            }
            return testData;
        }).orElseGet(() -> new DisabledTestData(Set.of(), Set.of()));
    }

    public void fillGroups(TestFrameworkImpl.TestsImpl tests) {
        for (final String id : new HashSet<>(enabledGroups)) {
            allChildGroups(tests.getOrCreateGroup(id)).forEach(gr -> enabledGroups.add(gr.id()));
        }
    }

    private static Stream<Group> allChildGroups(Group group) {
        return Stream.concat(Stream.of(group), group.entries().stream()
                        .filter(it -> it instanceof Group).flatMap(gr -> allChildGroups((Group) gr)))
                .distinct();
    }
}
