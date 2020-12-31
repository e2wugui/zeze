
namespace ConfigEditor
{
    partial class FormProjectConfig
    {
        /// <summary>
        /// Required designer variable.
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        /// Clean up any resources being used.
        /// </summary>
        /// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Windows Form Designer generated code

        /// <summary>
        /// Required method for Designer support - do not modify
        /// the contents of this method with the code editor.
        /// </summary>
        private void InitializeComponent()
        {
            this.gridConfig = new System.Windows.Forms.DataGridView();
            this.ConfigName = new System.Windows.Forms.DataGridViewTextBoxColumn();
            this.ConfigValue = new System.Windows.Forms.DataGridViewTextBoxColumn();
            ((System.ComponentModel.ISupportInitialize)(this.gridConfig)).BeginInit();
            this.SuspendLayout();
            // 
            // gridConfig
            // 
            this.gridConfig.AllowUserToAddRows = false;
            this.gridConfig.AllowUserToDeleteRows = false;
            this.gridConfig.AllowUserToOrderColumns = true;
            this.gridConfig.AllowUserToResizeRows = false;
            this.gridConfig.Anchor = ((System.Windows.Forms.AnchorStyles)((((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Bottom) 
            | System.Windows.Forms.AnchorStyles.Left) 
            | System.Windows.Forms.AnchorStyles.Right)));
            this.gridConfig.ColumnHeadersHeightSizeMode = System.Windows.Forms.DataGridViewColumnHeadersHeightSizeMode.AutoSize;
            this.gridConfig.Columns.AddRange(new System.Windows.Forms.DataGridViewColumn[] {
            this.ConfigName,
            this.ConfigValue});
            this.gridConfig.Location = new System.Drawing.Point(1, 2);
            this.gridConfig.Name = "gridConfig";
            this.gridConfig.RowHeadersWidth = 30;
            this.gridConfig.RowTemplate.Height = 23;
            this.gridConfig.Size = new System.Drawing.Size(819, 448);
            this.gridConfig.TabIndex = 0;
            this.gridConfig.CellEndEdit += new System.Windows.Forms.DataGridViewCellEventHandler(this.gridConfig_CellEndEdit);
            this.gridConfig.CellValidating += new System.Windows.Forms.DataGridViewCellValidatingEventHandler(this.gridConfig_CellValidating);
            // 
            // ConfigName
            // 
            this.ConfigName.HeaderText = "配置名";
            this.ConfigName.MinimumWidth = 10;
            this.ConfigName.Name = "ConfigName";
            this.ConfigName.ReadOnly = true;
            this.ConfigName.Width = 200;
            // 
            // ConfigValue
            // 
            this.ConfigValue.HeaderText = "配置参数";
            this.ConfigValue.MinimumWidth = 10;
            this.ConfigValue.Name = "ConfigValue";
            this.ConfigValue.Width = 550;
            // 
            // FormProjectConfig
            // 
            this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 12F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.ClientSize = new System.Drawing.Size(822, 450);
            this.Controls.Add(this.gridConfig);
            this.Name = "FormProjectConfig";
            this.ShowInTaskbar = false;
            this.StartPosition = System.Windows.Forms.FormStartPosition.CenterParent;
            this.Text = "FormProjectConfig";
            ((System.ComponentModel.ISupportInitialize)(this.gridConfig)).EndInit();
            this.ResumeLayout(false);

        }

        #endregion

        private System.Windows.Forms.DataGridView gridConfig;
        private System.Windows.Forms.DataGridViewTextBoxColumn ConfigName;
        private System.Windows.Forms.DataGridViewTextBoxColumn ConfigValue;
    }
}