module.exports = {

    uiPort: process.env.PORT || 1880,

    credentialSecret:
        "CHANGE_ME_SUPER_SECRET_NODE_RED_KEY",

    adminAuth: {
        type: "credentials",

        users: [{
            username: "admin",

            password:
                "$2b$08$HASH_PASSWORD",

            permissions: "*"
        }]
    },

    editorTheme: {

        projects: {
            enabled: false
        }
    },

    functionExternalModules: false,

    exportGlobalContextKeys: false
};