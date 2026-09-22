package com.dummytest

import de.hybris.bootstrap.annotations.UnitTest;
import org.junit.Test
import spock.lang.Specification


@UnitTest
class MockedLinkedList extends Specification {

    @Test
    def "test mocking"()
    {
        setup:
        // In this example mock the behaviour of LinkedList.
        // First step is to get a reference to a mocked LinkedList, by calling Mock(Class).
        def mockedList = Mock(LinkedList)
            {
                // Tell the mock that if at a later stage mockedList.get(0) is called,
                // the real implementation should NOT called,
                // but instead the value "first" should be returned
                get(0) >> "first"

                // Tell mock that if at a later stage mockedList.get(1) is called,
                // the real implementation should NOT called,
                // but instead a RuntimeException should be thrown
                get(1) >> {throw new RuntimeException()}
            }

        // Now if you call mockedList.get(0), you will get the value "first"
        when:
        def first = mockedList.get(0)
        then:
        first == "first"

        //If you call mockedList.get(1), exception will be thrown
        when:
        mockedList.get(1)
        then:
        thrown(RuntimeException)

        // Call the method mockedList.get(999), which has not been recorded,
        // and therefore mock's default value is returned
        when:
        def defaultValue = mockedList.get(999)
        then:
        defaultValue == null
    }
}
