package missive.common.protocol;

public enum PacketType {
    // auth
    AUTH_REQUEST, AUTH_RESPONSE,
    REGISTER_REQUEST, REGISTER_RESPONSE,
    LOGOUT,

    // rooms
    ROOM_LIST, ROOM_CREATED,
    JOIN_ROOM, CREATE_ROOM,
    ROOM_MEMBERS, START_DM,

    // search
    SEARCH_REQUEST, SEARCH_RESPONSE,

    // messages
    MESSAGE_SEND, MESSAGE_RECEIVE,
    HISTORY_REQUEST, HISTORY_RESPONSE,

    // users
    USER_JOINED, USER_LEFT,
    ONLINE_USERS,

    // keys
    PUBLIC_KEY_REQUEST, PUBLIC_KEY_RESPONSE,
    KEY_UPLOAD,

    // misc
    TYPING, STOP_TYPING,
    PING, PONG,
    ERROR
}
