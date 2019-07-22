package com.ykbjson.lib.simplepermission;

/**
 * Desription：Enum class to handle the different states
 *  of permissions since the PackageManager only
 *  has a granted and denied state.
 * Creator：yankebin
 * CreatedAt：2018/11/7
 */
enum Permissions {
  GRANTED,
  DENIED,
  NOT_FOUND,
  USER_DENIED_FOREVER
}