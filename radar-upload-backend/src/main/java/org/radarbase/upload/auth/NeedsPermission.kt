package org.radarbase.upload.auth

import org.radarcns.auth.authorization.Permission

/**
 * Indicates that a method needs an authenticated user that has a certain permission.
 */
@Target(AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class NeedsPermission(
        /**
         * Entity that the permission is needed on.
         */
        val entity: Permission.Entity,
        /**
         * Operation on given entity that the permission is needed for.
         */
        val operation: Permission.Operation)
