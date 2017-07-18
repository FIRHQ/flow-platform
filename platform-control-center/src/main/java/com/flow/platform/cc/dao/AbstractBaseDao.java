/*
 * Copyright 2017 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flow.platform.cc.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import java.io.Serializable;

/**
 * @author Will
 */
@Transactional
public abstract class AbstractBaseDao<K extends Serializable, T> implements BaseDao<K, T> {

    @FunctionalInterface
    interface Executable<O> {

        O execute(Session session);
    }

    <O> O execute(Executable<O> ex) {
        Session session = getSession();
        return ex.execute(session);
    }

    /**
     * Session Factory
     */
    @Autowired
    private SessionFactory sessionFactory;

    @Override
    public Session getSession() {
        return sessionFactory.getCurrentSession();
    }

    abstract Class<T> getEntityClass();

    /**
     * Get object by key
     *
     * @param key object id
     * @return T instance
     */
    @Override
    public T get(final K key) {
        return execute(session -> session.get(getEntityClass(), key));
    }

    /**
     * Save single object
     *
     * @return T with id if it is auto generated
     */
    @Override
    public T save(final T obj) {
        return execute(session -> {
            session.save(obj);
            return obj;
        });
    }

    /**
     * Update object
     */
    @Override
    public void update(final T obj) {
        execute(session -> {
            session.update(obj);
            return null;
        });
    }

    /**
     * Delete
     */
    @Override
    public void delete(T obj) {
        execute(session -> {
            session.delete(obj);
            return null;
        });
    }

    @Override
    public int deleteAll() {
        return execute(session -> {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaDelete<T> delete = builder.createCriteriaDelete(getEntityClass());
            delete.from(getEntityClass());
            return session.createQuery(delete).executeUpdate();
        });
    }
}
