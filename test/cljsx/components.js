export const JSComponent = props =>
  props.__hash === undefined
    ? "js"
    : "clj"

