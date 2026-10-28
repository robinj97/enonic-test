function json(status, body) {
    return {
        status: status,
        contentType: 'application/json',
        body: JSON.stringify(body)
    };
}

exports.get = function (req) {
    const params = req.params;

    if (!params.path) {
        return json(400, {error: 'Missing required parameter: path'});
    }

    const bean = __.newBean('com.example.inventory.InventoryBean');
    const result = __.toNativeObject(bean.count(params.project || 'default', params.branch || 'draft', params.path));

    // The bean returns null when the path does not exist.
    if (result === null || result === undefined) {
        return json(404, {error: 'Path not found: ' + params.path});
    }

    return json(200, result);
};
