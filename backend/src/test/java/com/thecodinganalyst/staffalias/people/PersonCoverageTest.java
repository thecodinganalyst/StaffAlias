package com.thecodinganalyst.staffalias.people;

import static org.assertj.core.api.Assertions.assertThat;

import com.thecodinganalyst.staffalias.tenant.Tenant;
import org.junit.jupiter.api.Test;

class PersonCoverageTest {

    @Test
    void constructorAndAccessorsAreCovered() {
        Tenant tenant = new Tenant("TENANT", "Tenant");
        Person person = new Person(tenant, "Alex", "Tan");

        assertThat(person.getId()).isNull();
        assertThat(person.getTenant()).isSameAs(tenant);
        assertThat(person.getFirstName()).isEqualTo("Alex");
        assertThat(person.getLastName()).isEqualTo("Tan");
    }
}
