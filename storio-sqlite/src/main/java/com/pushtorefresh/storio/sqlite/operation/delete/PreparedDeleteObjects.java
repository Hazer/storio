package com.pushtorefresh.storio.sqlite.operation.delete;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.pushtorefresh.storio.operation.internal.OnSubscribeExecuteAsBlocking;
import com.pushtorefresh.storio.sqlite.Changes;
import com.pushtorefresh.storio.sqlite.SQLiteTypeMapping;
import com.pushtorefresh.storio.sqlite.StorIOSQLite;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import rx.Observable;
import rx.schedulers.Schedulers;

import static com.pushtorefresh.storio.internal.Checks.checkNotNull;
import static com.pushtorefresh.storio.internal.Environment.throwExceptionIfRxJavaIsNotAvailable;

/**
 * Prepared Delete Operation for {@link StorIOSQLite}.
 *
 * @param <T> type of objects to delete.
 */
public final class PreparedDeleteObjects<T> extends PreparedDelete<DeleteResults<T>> {

    @NonNull
    private final Iterable<T> objects;

    @NonNull
    private final DeleteResolver<T> deleteResolver;

    private final boolean useTransaction;

    PreparedDeleteObjects(@NonNull StorIOSQLite storIOSQLite, @NonNull Iterable<T> objects, @NonNull DeleteResolver<T> deleteResolver, boolean useTransaction) {
        super(storIOSQLite);
        this.objects = objects;
        this.deleteResolver = deleteResolver;
        this.useTransaction = useTransaction;
    }

    /**
     * Executes Delete Operation immediately in current thread.
     *
     * @return non-null results of Delete Operation.
     */
    @NonNull
    @Override
    public DeleteResults<T> executeAsBlocking() {
        final StorIOSQLite.Internal internal = storIOSQLite.internal();

        final Map<T, DeleteResult> results = new HashMap<T, DeleteResult>();

        if (useTransaction) {
            internal.beginTransaction();
        }

        try {
            for (final T object : objects) {
                final DeleteResult deleteResult = deleteResolver.performDelete(storIOSQLite, object);

                results.put(
                        object,
                        deleteResult
                );

                if (!useTransaction) {
                    internal.notifyAboutChanges(Changes.newInstance(deleteResult.affectedTables()));
                }
            }

            if (useTransaction) {
                internal.setTransactionSuccessful();

                // if delete was in transaction and it was successful -> notify about changes

                final Set<String> affectedTables = new HashSet<String>(1); // in most cases it will be one table

                for (final T object : results.keySet()) {
                    affectedTables.addAll(results.get(object).affectedTables());
                }

                internal.notifyAboutChanges(Changes.newInstance(affectedTables));
            }
        } finally {
            if (useTransaction) {
                internal.endTransaction();
            }
        }

        return DeleteResults.newInstance(results);
    }

    /**
     * Creates {@link Observable} which will perform Delete Operation and send result to observer.
     * <p/>
     * Returned {@link Observable} will be "Cold Observable", which means that it performs
     * delete only after subscribing to it. Also, it emits the result once.
     * <p/>
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>Operates on {@link Schedulers#io()}.</dd>
     * </dl>
     *
     * @return non-null {@link Observable} which will perform Delete Operation.
     * and send result to observer.
     */
    @NonNull
    @Override
    public Observable<DeleteResults<T>> createObservable() {
        throwExceptionIfRxJavaIsNotAvailable("createObservable()");

        return Observable
                .create(OnSubscribeExecuteAsBlocking.newInstance(this))
                .subscribeOn(Schedulers.io());
    }

    /**
     * Builder for {@link PreparedDeleteObjects}.
     *
     * @param <T> type of objects to delete.
     */
    public static final class Builder<T> {

        @NonNull
        private final StorIOSQLite storIOSQLite;

        @NonNull
        private final Class<T> type;

        @NonNull
        private final Iterable<T> objects;

        private DeleteResolver<T> deleteResolver;

        private boolean useTransaction = true;

        Builder(@NonNull StorIOSQLite storIOSQLite, @NonNull Class<T> type, @NonNull Iterable<T> objects) {
            this.storIOSQLite = storIOSQLite;
            this.type = type;
            this.objects = objects;
        }


        /**
         * Optional: Defines that Delete Operation will use transaction or not.
         * <p/>
         * By default, transaction will be used.
         *
         * @param useTransaction {@code true} to use transaction, {@code false} to not.
         * @return builder.
         */
        @NonNull
        public Builder<T> useTransaction(boolean useTransaction) {
            this.useTransaction = useTransaction;
            return this;
        }

        /**
         * Optional: Specifies {@link DeleteResolver} for Delete Operation.
         * <p/>
         * <p/>
         * Can be set via {@link SQLiteTypeMapping},
         * If value is not set via {@link SQLiteTypeMapping}
         * or explicitly -> exception will be thrown.
         *
         * @param deleteResolver {@link DeleteResolver} for Delete Operation.
         * @return builder.
         */
        @NonNull
        public Builder<T> withDeleteResolver(@Nullable DeleteResolver<T> deleteResolver) {
            this.deleteResolver = deleteResolver;
            return this;
        }

        /**
         * Prepares Delete Operation.
         *
         * @return {@link PreparedDeleteObjects}.
         */
        @NonNull
        public PreparedDeleteObjects<T> prepare() {
            final SQLiteTypeMapping<T> typeMapping = storIOSQLite.internal().typeMapping(type);

            if (deleteResolver == null && typeMapping != null) {
                deleteResolver = typeMapping.deleteResolver();
            }

            checkNotNull(deleteResolver, "Please specify DeleteResolver");

            return new PreparedDeleteObjects<T>(
                    storIOSQLite,
                    objects,
                    deleteResolver,
                    useTransaction
            );
        }
    }
}
